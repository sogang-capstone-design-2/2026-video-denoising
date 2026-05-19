package com.nightvision.service;

import com.nightvision.model.Job;
import com.nightvision.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DenoiseServiceTest {

    @Mock
    private JobProcessor jobProcessor;

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private DenoiseService denoiseService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(denoiseService, "uploadDir", tempDir.toString());
    }

    // ──────────────────────────────────────────
    // submit()
    // ──────────────────────────────────────────

    @Test
    void 파일제출시_jobId를_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        String jobId = denoiseService.submit(file, "general", "medium");

        assertThat(jobId).isNotNull();
    }

    @Test
    void 파일제출시_job을_DB에_저장한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        denoiseService.submit(file, "general", "medium");

        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void 파일제출시_비동기_처리를_시작한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        denoiseService.submit(file, "general", "medium");

        verify(jobProcessor).process(any(Job.class), any(Path.class), eq("medium"));
    }

    @Test
    void 파일제출시_mode가_job에_정확히_전달된다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        String jobId = denoiseService.submit(file, "lowlight", "high");
        Job job = denoiseService.getJob(jobId);

        assertThat(job.getMode()).isEqualTo("lowlight");
    }

    // ──────────────────────────────────────────
    // getJob()
    // ──────────────────────────────────────────

    @Test
    void 처리중인_job_조회시_DB없이_메모리에서_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());
        String jobId = denoiseService.submit(file, "general", "medium");

        Job result = denoiseService.getJob(jobId);

        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(jobId);
        verify(jobRepository, never()).findById(jobId);
    }

    @Test
    void 메모리에_없는_job_조회시_DB에서_조회한다() {
        Job dbJob = new Job("old-job", "general", "old.mp4");
        given(jobRepository.findById("old-job")).willReturn(Optional.of(dbJob));

        Job result = denoiseService.getJob("old-job");

        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo("old-job");
        verify(jobRepository).findById("old-job");
    }

    @Test
    void 존재하지않는_jobId_조회시_null을_반환한다() {
        given(jobRepository.findById("unknown")).willReturn(Optional.empty());

        Job result = denoiseService.getJob("unknown");

        assertThat(result).isNull();
    }

    // ──────────────────────────────────────────
    // getRecentJobs()
    // ──────────────────────────────────────────

    @Test
    void 최근작업_조회시_repository에_위임한다() {
        given(jobRepository.findTop10ByOrderByCreatedAtDesc()).willReturn(List.of());

        denoiseService.getRecentJobs();

        verify(jobRepository).findTop10ByOrderByCreatedAtDesc();
    }
}
