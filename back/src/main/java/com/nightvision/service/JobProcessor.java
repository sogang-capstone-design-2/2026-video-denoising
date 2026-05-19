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
 * @Async가 동일 클래스 내 자가 호출 시 프록시를 우회하는 문제를 피하기 위해
 * DenoiseService와 분리된 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class JobProcessor {

    private final PythonInferenceClient inferenceClient;
    private final JobRepository jobRepository;

    @Value("${app.result-dir}")
    private String resultDir;

    @Async("denoiseExecutor")
    public void process(Job job, Path inputPath, String intensity) {
        try {
            job.setPhase(Job.Phase.UPLOADING);
            job.setPercent(30);

            job.setPhase(Job.Phase.PROCESSING);
            job.setPercent(35);

            byte[] resultBytes = inferenceClient.infer(inputPath, job.getMode(), intensity);

            job.setPercent(90);

            job.setPhase(Job.Phase.FINALIZING);
            job.setPercent(95);

            Path resultPath = saveResult(job.getJobId(), job.getOriginalFileName(), resultBytes);
            job.setResultPath(resultPath.toString());

            job.setPercent(100);
            job.setPhase(Job.Phase.DONE);

        } catch (Exception e) {
            job.setPhase(Job.Phase.FAILED);
            job.setErrorMessage(e.getMessage());

        } finally {
            // 처리 완료(성공/실패) 시 최종 상태를 DB에 저장
            jobRepository.save(job);
        }
    }

    private Path saveResult(String jobId, String originalFileName, byte[] bytes) throws IOException {
        Path dir = Path.of(resultDir, jobId);
        Files.createDirectories(dir);
        Path dest = dir.resolve("denoised_" + originalFileName);
        Files.write(dest, bytes);
        return dest;
    }
}
