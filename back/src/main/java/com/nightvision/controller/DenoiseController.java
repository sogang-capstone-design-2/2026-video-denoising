package com.nightvision.controller;

import com.nightvision.model.Job;
import com.nightvision.service.DenoiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
     * POST /api/denoise/{mode}
     * 파일 업로드 → jobId 즉시 반환 (비동기 처리 시작)
     */
    @PostMapping("/denoise/{mode}")
    public ResponseEntity<Map<String, String>> upload(
            @PathVariable String mode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "medium") String intensity
    ) {
        String jobId = denoiseService.submit(file, mode, intensity);
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    /**
     * GET /api/jobs/{jobId}/status
     * 처리 상태 폴링
     * Response: { "phase": "processing", "percent": 65 }
     *           { "phase": "done",       "percent": 100, "resultUrl": "/api/jobs/{jobId}/file" }
     *           { "phase": "failed",     "error": "..." }
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
