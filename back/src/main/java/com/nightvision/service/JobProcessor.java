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
 * 비동기 추론 실행기
 *
 * QUEUED ──▶ UPLOADING ──▶ PROCESSING ──▶ FINALIZING ──▶ DONE
 *                               │
 *                               └─────▶ FAILED
 *
 * mode에 따라 분기:
 *   general  → POST /denoise/video → denoised mp4
 *   lowlight → POST /visualize/raw (noisy PNG 먼저) → POST /denoise/raw (denoised PNG)
 */
@Component
@RequiredArgsConstructor
public class JobProcessor {

    private final InferenceClient inferenceClient;
    private final JobRepository jobRepository;

    @Value("${app.result-dir}")
    private String resultDir;

    @Async("denoiseExecutor")
    public void runInference(Job job, Path inputPath) {
        try {
            transition(job, Job.Phase.UPLOADING, 10);
            transition(job, Job.Phase.UPLOADING, 30);
            transition(job, Job.Phase.PROCESSING, 35);

            if ("lowlight".equals(job.getMode())) {
                runRaw(job, inputPath);
            } else {
                runVideo(job, inputPath);
            }

        } catch (Exception e) {
            job.setErrorMessage(e.getMessage());
            transition(job, Job.Phase.FAILED, job.getPercent());
        }
    }

    // ── general: FastDVDnet ───────────────────────────────────────────────

    private void runVideo(Job job, Path inputPath) throws IOException {
        byte[] resultBytes = inferenceClient.denoise(
                inputPath, job.getNoiseSigma(), job.isAddNoise(), job.isCompare());

        transition(job, Job.Phase.PROCESSING, 80);
        transition(job, Job.Phase.FINALIZING, 90);

        Path resultPath = saveFile(job.getJobId(), "denoised_" + job.getOriginalFileName(), resultBytes);
        job.setResultPath(resultPath.toString());

        transition(job, Job.Phase.DONE, 100);
    }

    // ── lowlight: RViDeNet ────────────────────────────────────────────────

    private void runRaw(Job job, Path inputPath) throws IOException {
        // 1단계: /visualize/raw — 추론 없이 noisy PNG 빠르게 생성
        byte[] noisyBytes = inferenceClient.visualizeRaw(inputPath);
        Path noisyPath = saveFile(job.getJobId(), "noisy.png", noisyBytes);
        job.setNoisyImagePath(noisyPath.toString());
        jobRepository.save(job); // before 이미지 먼저 DB 반영

        transition(job, Job.Phase.PROCESSING, 55);

        // 2단계: /denoise/raw — RViDeNet 추론으로 denoised PNG 생성
        byte[] denoisedBytes = inferenceClient.denoiseRaw(inputPath);

        transition(job, Job.Phase.PROCESSING, 80);
        transition(job, Job.Phase.FINALIZING, 90);

        Path denoisedPath = saveFile(job.getJobId(), "denoised.png", denoisedBytes);
        job.setResultPath(denoisedPath.toString());

        transition(job, Job.Phase.DONE, 100);
    }

    // ── 공통 유틸 ─────────────────────────────────────────────────────────

    private void transition(Job job, Job.Phase phase, int percent) {
        job.setPhase(phase);
        job.setPercent(percent);
        jobRepository.save(job);
    }

    private Path saveFile(String jobId, String fileName, byte[] bytes) throws IOException {
        Path dir = Path.of(resultDir, jobId);
        Files.createDirectories(dir);
        Path dest = dir.resolve(fileName);
        Files.write(dest, bytes);
        return dest;
    }
}
