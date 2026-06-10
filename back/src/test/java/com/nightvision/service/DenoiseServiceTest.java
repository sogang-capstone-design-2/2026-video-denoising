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
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
    // submit() — general 모드
    // ──────────────────────────────────────────

    @Test
    void general_파일제출시_jobId를_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        String jobId = denoiseService.submit(file, "general", 25f, false, false);

        assertThat(jobId).isNotNull();
    }

    @Test
    void general_파일제출시_QUEUED_상태로_DB에_저장한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        String jobId = denoiseService.submit(file, "general", 25f, false, false);
        Job job = denoiseService.getJob(jobId);

        assertThat(job.getPhase()).isEqualTo(Job.Phase.QUEUED);
        // QUEUED 저장 + inputPath 업데이트 = 2회
        verify(jobRepository, times(2)).save(any(Job.class));
    }

    @Test
    void general_파일제출시_inputPath가_job에_저장된다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        String jobId = denoiseService.submit(file, "general", 25f, false, false);
        Job job = denoiseService.getJob(jobId);

        assertThat(job.getInputPath()).isNotNull();
    }

    @Test
    void general_파일제출시_비동기_추론을_시작한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        denoiseService.submit(file, "general", 25f, false, false);

        verify(jobProcessor).runInference(any(Job.class), any(Path.class));
    }

    @Test
    void general_파라미터가_job에_정확히_전달된다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());

        String jobId = denoiseService.submit(file, "general", 30f, true, true);
        Job job = denoiseService.getJob(jobId);

        assertThat(job.getMode()).isEqualTo("general");
        assertThat(job.getNoiseSigma()).isEqualTo(30f);
        assertThat(job.isAddNoise()).isTrue();
        assertThat(job.isCompare()).isTrue();
    }

    // ──────────────────────────────────────────
    // submit() — lowlight 모드
    // ──────────────────────────────────────────

    @Test
    void lowlight_파일제출시_jobId를_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "frame.raw", "application/octet-stream", "data".getBytes());

        String jobId = denoiseService.submit(file, "lowlight", 25f, false, false);

        assertThat(jobId).isNotNull();
    }

    @Test
    void lowlight_파일제출시_mode가_lowlight로_설정된다() {
        MockMultipartFile file = new MockMultipartFile("file", "frame.raw", "application/octet-stream", "data".getBytes());

        String jobId = denoiseService.submit(file, "lowlight", 25f, false, false);
        Job job = denoiseService.getJob(jobId);

        assertThat(job.getMode()).isEqualTo("lowlight");
    }

    @Test
    void lowlight_파일제출시_비동기_추론을_시작한다() {
        MockMultipartFile file = new MockMultipartFile("file", "frame.raw", "application/octet-stream", "data".getBytes());

        denoiseService.submit(file, "lowlight", 25f, false, false);

        verify(jobProcessor).runInference(any(Job.class), any(Path.class));
    }

    // ──────────────────────────────────────────
    // getJob()
    // ──────────────────────────────────────────

    @Test
    void 처리중인_job_조회시_DB없이_메모리에서_반환한다() {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "data".getBytes());
        String jobId = denoiseService.submit(file, "general", 25f, false, false);

        Job result = denoiseService.getJob(jobId);

        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(jobId);
        verify(jobRepository, never()).findById(jobId);
    }

    @Test
    void 메모리에_없는_job_조회시_DB에서_조회한다() {
        Job dbJob = new Job("old-job", "old.mp4", "general", 25f, false, false);
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
    // getJobs()
    // ──────────────────────────────────────────

    @Test
    void limit으로_필터링된_목록을_반환한다() {
        Job job = new Job("job-1", "test.mp4", "general", 25f, false, false);
        given(jobRepository.findJobsWithFilter(any(Pageable.class))).willReturn(List.of(job));

        List<Job> result = denoiseService.getJobs(5);

        assertThat(result).hasSize(1);
        verify(jobRepository).findJobsWithFilter(any(Pageable.class));
    }

    @Test
    void limit이_50을_초과하면_50으로_제한된다() {
        given(jobRepository.findJobsWithFilter(any(Pageable.class))).willReturn(List.of());

        denoiseService.getJobs(100);

        verify(jobRepository).findJobsWithFilter(
                argThat(pageable -> pageable.getPageSize() == 50)
        );
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
