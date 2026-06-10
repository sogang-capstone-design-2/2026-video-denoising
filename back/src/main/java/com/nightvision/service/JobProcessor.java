package com.nightvision.service;

import com.nightvision.model.Job;
import com.nightvision.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 비동기 추론 실행기
 *
 * QUEUED ──▶ UPLOADING ──▶ PROCESSING ──▶ FINALIZING ──▶ DONE
 *                               │
 *                               └─────▶ FAILED
 *
 * mode에 따라 분기:
 *   general  → POST /denoise      → denoised mp4 저장
 *   lowlight → POST /denoise/raw  → ZIP 추출 → noisy.png / denoised.png 저장
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
        byte[] zipBytes = inferenceClient.denoiseRaw(inputPath);

        transition(job, Job.Phase.PROCESSING, 80);
        transition(job, Job.Phase.FINALIZING, 90);

        // ZIP에서 noisy.png / denoised.png 추출
        Map<String, byte[]> entries = extractZip(zipBytes);

        byte[] noisyBytes    = entries.get("noisy.png");
        byte[] denoisedBytes = entries.get("denoised.png");

        if (noisyBytes == null || denoisedBytes == null) {
            throw new RuntimeException("추론 서버 ZIP 응답에 noisy.png / denoised.png 가 없습니다.");
        }

        Path noisyPath    = saveFile(job.getJobId(), "noisy.png",    noisyBytes);
        Path denoisedPath = saveFile(job.getJobId(), "denoised.png", denoisedBytes);

        // before 요청 시 시각화된 noisy.png 제공
        job.setNoisyImagePath(noisyPath.toString());
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

    private Map<String, byte[]> extractZip(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
                zis.closeEntry();
            }
        }
        return entries;
    }
}
