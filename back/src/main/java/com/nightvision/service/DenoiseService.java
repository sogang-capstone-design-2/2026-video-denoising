package com.nightvision.service;

import com.nightvision.model.Job;
import com.nightvision.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DenoiseService {

    private final JobProcessor jobProcessor;
    private final JobRepository jobRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    // 처리 중인 Job은 메모리에 유지 → 폴링 응답 속도 확보
    private final Map<String, Job> activeJobs = new ConcurrentHashMap<>();

    /**
     * S2.1 — 파일 저장 → QUEUED 상태로 DB 저장 → 비동기 처리 시작 → jobId 반환
     */
    public String submit(MultipartFile file, String mode, float noiseSigma, boolean addNoise, boolean compare) {
        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, file.getOriginalFilename(), mode, noiseSigma, addNoise, compare);
        job.setPhase(Job.Phase.QUEUED);

        activeJobs.put(jobId, job);
        jobRepository.save(job);

        try {
            Path inputPath = saveUploadedFile(jobId, file);
            job.setInputPath(inputPath.toString());
            jobRepository.save(job);
            jobProcessor.runInference(job, inputPath);
        } catch (IOException e) {
            job.setPhase(Job.Phase.FAILED);
            job.setErrorMessage("파일 저장 실패: " + e.getMessage());
            jobRepository.save(job);
        }

        return jobId;
    }

    public Job getJob(String jobId) {
        // 처리 중이면 메모리에서 (빠름), 완료 후 재조회면 DB에서
        Job job = activeJobs.get(jobId);
        if (job != null) return job;
        return jobRepository.findById(jobId).orElse(null);
    }

    /**
     */
    public List<Job> getJobs(int limit) {
        int clampedLimit = Math.min(limit, 50);
        return jobRepository.findJobsWithFilter(PageRequest.of(0, clampedLimit));
    }

    public List<Job> getRecentJobs() {
        return jobRepository.findTop10ByOrderByCreatedAtDesc();
    }

    private Path saveUploadedFile(String jobId, MultipartFile file) throws IOException {
        Path dir = Path.of(uploadDir, jobId);
        Files.createDirectories(dir);
        Path dest = dir.resolve(file.getOriginalFilename());
        file.transferTo(dest);
        return dest;
    }
}
