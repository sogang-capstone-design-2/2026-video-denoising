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

import static org.mockito.ArgumentMatchers.*;
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
    // POST /api/jobs
    // ──────────────────────────────────────────

    @Test
    void 파일업로드시_201과_jobId를_반환한다() throws Exception {
        given(denoiseService.submit(any(), anyFloat(), anyBoolean(), anyBoolean())).willReturn("test-job-id");

        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "content".getBytes());

        mockMvc.perform(multipart("/api/jobs")
                        .file(file)
                        .param("noiseSigma", "25")
                        .param("addNoise", "false")
                        .param("compare", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value("test-job-id"));
    }

    @Test
    void 파일업로드시_파라미터_생략하면_기본값으로_처리된다() throws Exception {
        given(denoiseService.submit(any(), anyFloat(), anyBoolean(), anyBoolean())).willReturn("test-job-id");

        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "content".getBytes());

        mockMvc.perform(multipart("/api/jobs").file(file))
                .andExpect(status().isCreated())
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
    void 대기중인_job_상태조회시_queued_phase를_반환한다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.QUEUED);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("queued"))
                .andExpect(jsonPath("$.percent").value(0));
    }

    @Test
    void 처리중인_job_상태조회시_phase와_percent를_반환한다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
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
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.DONE);
        job.setPercent(100);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultUrl").value("/api/jobs/job-1/result"));
    }

    @Test
    void 처리실패한_job_상태조회시_에러메시지가_포함된다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.FAILED);
        job.setErrorMessage("Python 서버 연결 실패");
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("failed"))
                .andExpect(jsonPath("$.error").value("Python 서버 연결 실패"));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs/{jobId}/result
    // ──────────────────────────────────────────

    @Test
    void 존재하지않는_jobId로_결과조회시_404를_반환한다() throws Exception {
        given(denoiseService.getJob("unknown")).willReturn(null);

        mockMvc.perform(get("/api/jobs/unknown/result"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 처리중인_job_결과조회시_202를_반환한다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.PROCESSING);
        job.setPercent(60);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/result"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.phase").value("processing"))
                .andExpect(jsonPath("$.percent").value(60));
    }

    @Test
    void 처리완료된_job_결과조회시_200과_메타데이터를_반환한다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 30f, true, false);
        job.setPhase(Job.Phase.DONE);
        job.setPercent(100);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.noiseSigma").value(30.0))
                .andExpect(jsonPath("$.addNoise").value(true))
                .andExpect(jsonPath("$.compare").value(false))
                .andExpect(jsonPath("$.fileName").value("test.mp4"))
                .andExpect(jsonPath("$.beforeUrl").value("/api/jobs/job-1/files/before"))
                .andExpect(jsonPath("$.afterUrl").value("/api/jobs/job-1/files/after"));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs/{jobId}/files/before
    // ──────────────────────────────────────────

    @Test
    void inputPath가_없는_job의_before파일_요청시_404를_반환한다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/files/before"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 처리중인_job의_before파일_요청시_원본파일을_반환한다(@TempDir Path tempDir) throws Exception {
        Path inputFile = tempDir.resolve("test.mp4");
        Files.write(inputFile, "original video".getBytes());

        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.PROCESSING);
        job.setInputPath(inputFile.toString());
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/files/before"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.mp4\""));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs/{jobId}/files/after
    // ──────────────────────────────────────────

    @Test
    void 처리중인_job의_after파일_요청시_404를_반환한다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.PROCESSING);
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/files/after"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 처리완료된_job의_after파일_요청시_denoised_접두사로_반환한다(@TempDir Path tempDir) throws Exception {
        Path resultFile = tempDir.resolve("denoised_test.mp4");
        Files.write(resultFile, "denoised video".getBytes());

        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        job.setPhase(Job.Phase.DONE);
        job.setResultPath(resultFile.toString());
        given(denoiseService.getJob("job-1")).willReturn(job);

        mockMvc.perform(get("/api/jobs/job-1/files/after"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"denoised_test.mp4\""));
    }

    // ──────────────────────────────────────────
    // GET /api/jobs
    // ──────────────────────────────────────────

    @Test
    void 작업목록_조회시_기본_10개를_반환한다() throws Exception {
        given(denoiseService.getJobs(10)).willReturn(List.of());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void 작업목록_조회시_limit_파라미터가_적용된다() throws Exception {
        Job job = new Job("job-1", "test.mp4", 25f, false, false);
        given(denoiseService.getJobs(5)).willReturn(List.of(job));

        mockMvc.perform(get("/api/jobs").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].noiseSigma").value(25.0));
    }
}
