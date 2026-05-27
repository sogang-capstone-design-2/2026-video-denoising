package com.nightvision.service;

import com.nightvision.model.Job;
import com.nightvision.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * S2.3 — JobService.runInference()
 * 비동기 처리, 상태 전이 5단계 DB 반영 + 실패 시 FAILED
 *
 * QUEUED ──▶ UPLOADING ──▶ PROCESSING ──▶ FINALIZING ──▶ DONE
 *                               │
 *                               └─────▶ FAILED
 *
 * @Async가 동일 클래스 내 자가 호출 시 프록시를 우회하는 문제를 피하기 위해
 * DenoiseService와 분리된 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class JobProcessor {

    private final InferenceClient inferenceClient;
    private final JobRepository jobRepository;

    @Value("${app.result-dir}")
    private String resultDir;

    @Async("denoiseExecutor")
    public void runInference(Job job, Path inputPath, String intensity) {
        try {
            // 1. QUEUED → UPLOADING
            transition(job, Job.Phase.UPLOADING, 10);

            // 2. 업로드 완료
            transition(job, Job.Phase.UPLOADING, 30);

            // 3. UPLOADING → PROCESSING : Python 추론 서버 호출
            transition(job, Job.Phase.PROCESSING, 35);

            byte[] resultBytes = inferenceClient.infer(inputPath, job.getMode(), intensity);

            transition(job, Job.Phase.PROCESSING, 80);

            // 4. PROCESSING → FINALIZING : 결과 파일 저장
            transition(job, Job.Phase.FINALIZING, 90);

            Path resultPath = saveResult(job.getJobId(), job.getOriginalFileName(), resultBytes);
            job.setResultPath(resultPath.toString());

            // 5. FINALIZING → DONE
            transition(job, Job.Phase.DONE, 100);

        } catch (Exception e) {
            job.setErrorMessage(e.getMessage());
            transition(job, Job.Phase.FAILED, job.getPercent());
        }
    }

    /**
     * 상태 전이 + DB 즉시 저장
     * 폴링 요청이 항상 최신 상태를 읽을 수 있도록 매 단계마다 persist.
     */
    private void transition(Job job, Job.Phase phase, int percent) {
        job.setPhase(phase);
        job.setPercent(percent);
        jobRepository.save(job);
    }

    private Path saveResult(String jobId, String originalFileName, byte[] bytes) throws IOException {
        Path dir = Path.of(resultDir, jobId);
        Files.createDirectories(dir);
        Path dest = dir.resolve("denoised_" + originalFileName);
        Files.write(dest, bytes);
        return dest;
    }
}
