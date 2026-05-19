package com.nightvision.controller;

import com.nightvision.model.Job;
import com.nightvision.service.DenoiseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DenoiseController.class)
class DenoiseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DenoiseService denoiseService;

    // ──────────────────────────────────────────
    // POST /api/denoise/{mode}
    // ──────────────────────────────────────────

    @Test
    void 파일업로드시_jobId를_반환한다() throws Exception {
        given(denoiseService.submit(any(), anyString(), anyString())).willReturn("test-job-id");

        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "content".getBytes());

        mockMvc.perform(multipart("/api/denoise/general").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-id"));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs/{jobId}/status
    // ──────────────────────────────────────────

    @Test
    void 존재하지않는_jobId로_상태조회시_404를_반환한다() throws Exception {
        given(denoiseService.getJob("unknown")).willReturn(null);

        mockMvc.perform(get("/api/jobs/unknown/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 처리중인_job_상태조회시_phase와_percent를_반환한다() throws Exception {
        Job job = new Job("job-1", "general", "test.mp4");
        job.setPhase(Job.Phase.PROCESSING);
        job.setPercent(50);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("processing"))
                .andExpect(jsonPath("$.percent").value(50));
    }

    @Test
    void 처리완료된_job_상태조회시_resultUrl이_포함된다() throws Exception {
        Job job = new Job("job-1", "general", "test.mp4");
        job.setPhase(Job.Phase.DONE);
        job.setPercent(100);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultUrl").value("/api/jobs/job-1/file"));
    }

    @Test
    void 처리실패한_job_상태조회시_에러메시지가_포함된다() throws Exception {
        Job job = new Job("job-1", "general", "test.mp4");
        job.setPhase(Job.Phase.FAILED);
        job.setErrorMessage("Python 서버 연결 실패");
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("failed"))
                .andExpect(jsonPath("$.error").value("Python 서버 연결 실패"));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs/{jobId}/file
    // ──────────────────────────────────────────

    @Test
    void 처리중인_job의_파일요청시_404를_반환한다() throws Exception {
        Job job = new Job("job-1", "general", "test.mp4");
        job.setPhase(Job.Phase.PROCESSING);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/file"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 처리완료된_job의_파일요청시_파일을_반환한다(@TempDir Path tempDir) throws Exception {
        Path resultFile = tempDir.resolve("denoised_test.mp4");
        Files.write(resultFile, "fake video content".getBytes());

        Job job = new Job("job-1", "general", "test.mp4");
        job.setPhase(Job.Phase.DONE);
        job.setResultPath(resultFile.toString());
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"denoised_test.mp4\""));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs/recent
    // ──────────────────────────────────────────

    @Test
    void 최근작업_조회시_빈목록을_반환한다() throws Exception {
        given(denoiseService.getRecentJobs()).willReturn(List.of());

        mockMvc.perform(get("/api/jobs/recent"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void 최근작업_조회시_작업목록을_반환한다() throws Exception {
        Job job = new Job("job-1", "general", "test.mp4");
        given(denoiseService.getRecentJobs()).willReturn(List.of(job));

        mockMvc.perform(get("/api/jobs/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value("job-1"));
    }
}
