package com.nightvision.controller;

import com.nightvision.model.Job;
import com.nightvision.service.DenoiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DenoiseController {

    private final DenoiseService denoiseService;

    /**
     * S2.1 — POST /api/jobs
     * 파일 업로드 → DB 저장(QUEUED) → 비동기 처리 시작 → 201 반환
     */
    @PostMapping("/jobs")
    public ResponseEntity<Map<String, String>> createJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam String mode,
            @RequestParam(defaultValue = "medium") String intensity
    ) {
        String jobId = denoiseService.submit(file, mode, intensity);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", jobId));
    }

    /**
     * S2.4 — GET /api/jobs/{id}/status
     * 폴링용 상태 조회
     * Response: { "phase": "processing", "percent": 65 }
     *           { "phase": "done",       "percent": 100, "resultUrl": "/api/jobs/{id}/file" }
     *           { "phase": "failed",     "percent": N,   "error": "..." }
     */
    @GetMapping("/jobs/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String jobId) {
        Job job = denoiseService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("phase", job.getPhase().name().toLowerCase());
        response.put("percent", job.getPercent());

        if (job.isDone()) {
            response.put("resultUrl", "/api/jobs/" + jobId + "/file");
        }
        if (job.isFailed()) {
            response.put("error", job.getErrorMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/jobs/{jobId}/file
     * 처리 완료된 결과 파일 다운로드
     */
    @GetMapping("/jobs/{jobId}/file")
    public ResponseEntity<Resource> getResultFile(@PathVariable String jobId) {
        Job job = denoiseService.getJob(jobId);
        if (job == null || !job.isDone() || job.getResultPath() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(Path.of(job.getResultPath()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"denoised_" + job.getOriginalFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * GET /api/jobs/recent
     * 최근 작업 목록 10개 (프론트 "최근 작업" 패널용)
     */
    @GetMapping("/jobs/recent")
    public ResponseEntity<List<Job>> getRecentJobs() {
        return ResponseEntity.ok(denoiseService.getRecentJobs());
    }
}
