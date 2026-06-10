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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DenoiseController {

    private final DenoiseService denoiseService;

    // ──────────────────────────────────────────
    // S2.1 — POST /api/jobs
    // ──────────────────────────────────────────

    @PostMapping("/jobs")
    public ResponseEntity<Map<String, String>> createJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String mode,
            @RequestParam(defaultValue = "25") float noiseSigma,
            @RequestParam(defaultValue = "false") boolean addNoise,
            @RequestParam(defaultValue = "false") boolean compare
    ) {
        String jobId = denoiseService.submit(file, mode, noiseSigma, addNoise, compare);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("jobId", jobId));
    }

    // ──────────────────────────────────────────
    // S2.4 — GET /api/jobs/{id}/status
    // ──────────────────────────────────────────

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
            response.put("resultUrl", "/api/jobs/" + jobId + "/result");
        }
        if (job.isFailed()) {
            response.put("error", job.getErrorMessage());
        }

        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────
    // S3.1 — GET /api/jobs/{id}/result
    // ──────────────────────────────────────────

    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<Map<String, Object>> getResult(@PathVariable String jobId) {
        Job job = denoiseService.getJob(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        // 처리 중 → 202 Accepted
        if (!job.isDone()) {
            Map<String, Object> response = new HashMap<>();
            response.put("phase", job.getPhase().name().toLowerCase());
            response.put("percent", job.getPercent());
            return ResponseEntity.accepted().body(response);
        }

        // 완료 → 200 OK + 메타데이터
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", job.getJobId());
        result.put("mode", job.getMode());
        result.put("noiseSigma", job.getNoiseSigma());
        result.put("addNoise", job.isAddNoise());
        result.put("compare", job.isCompare());
        result.put("fileName", job.getOriginalFileName());
        result.put("createdAt", job.getCreatedAt());
        result.put("beforeUrl", "/api/jobs/" + jobId + "/files/before");
        result.put("afterUrl",  "/api/jobs/" + jobId + "/files/after");
        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────
    // S3.2 — GET /api/jobs/{id}/files/before
    // ──────────────────────────────────────────

    @GetMapping("/jobs/{jobId}/files/before")
    public ResponseEntity<Resource> getBeforeFile(@PathVariable String jobId) {
        Job job = denoiseService.getJob(jobId);
        if (job == null) return ResponseEntity.notFound().build();

        // lowlight: noisyImagePath(noisy.png) 우선, 없으면 원본 RAW
        String rawPath = job.getNoisyImagePath() != null
                ? job.getNoisyImagePath()
                : job.getInputPath();
        if (rawPath == null) return ResponseEntity.notFound().build();

        Path filePath = Path.of(rawPath);
        String fileName = filePath.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodeFilename(fileName))
                .contentType(MediaType.parseMediaType(detectContentType(filePath)))
                .body(new FileSystemResource(filePath));
    }

    // ──────────────────────────────────────────
    // S3.3 — GET /api/jobs/{id}/files/after
    // ──────────────────────────────────────────

    @GetMapping("/jobs/{jobId}/files/after")
    public ResponseEntity<Resource> getAfterFile(@PathVariable String jobId) {
        Job job = denoiseService.getJob(jobId);
        if (job == null || !job.isDone() || job.getResultPath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Path.of(job.getResultPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodeFilename("denoised_" + job.getOriginalFileName()))
                .contentType(MediaType.parseMediaType(detectContentType(filePath)))
                .body(new FileSystemResource(filePath));
    }

    // ──────────────────────────────────────────
    // S3.4 — GET /api/jobs
    // ──────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getJobs(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(denoiseService.getJobs(limit));
    }

    // ──────────────────────────────────────────
    // 공통 유틸
    // ──────────────────────────────────────────

    private String detectContentType(Path filePath) {
        try {
            String type = java.nio.file.Files.probeContentType(filePath);
            return type != null ? type : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
