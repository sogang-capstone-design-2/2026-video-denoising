package com.nightvision.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobTest {

    // ──────────────────────────────────────────
    // 생성 및 초기 상태
    // ──────────────────────────────────────────

    @Test
    void general_job_생성시_초기상태가_올바르게_설정된다() {
        Job job = new Job("id-1", "test.mp4", "general", 25f, false, false);

        assertThat(job.getJobId()).isEqualTo("id-1");
        assertThat(job.getOriginalFileName()).isEqualTo("test.mp4");
        assertThat(job.getMode()).isEqualTo("general");
        assertThat(job.getNoiseSigma()).isEqualTo(25f);
        assertThat(job.isAddNoise()).isFalse();
        assertThat(job.isCompare()).isFalse();
        assertThat(job.getPhase()).isEqualTo(Job.Phase.QUEUED);
        assertThat(job.getPercent()).isEqualTo(0);
        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    void lowlight_job_생성시_mode가_lowlight로_설정된다() {
        Job job = new Job("id-2", "frame.raw", "lowlight", 25f, false, false);

        assertThat(job.getMode()).isEqualTo("lowlight");
        assertThat(job.getOriginalFileName()).isEqualTo("frame.raw");
        assertThat(job.getNoisyImagePath()).isNull();
        assertThat(job.getResultPath()).isNull();
    }

    @Test
    void lowlight_job에_noisyImagePath를_설정할_수_있다() {
        Job job = new Job("id-2", "frame.raw", "lowlight", 25f, false, false);
        job.setNoisyImagePath("/results/id-2/noisy.png");
        job.setResultPath("/results/id-2/denoised.png");

        assertThat(job.getNoisyImagePath()).isEqualTo("/results/id-2/noisy.png");
        assertThat(job.getResultPath()).isEqualTo("/results/id-2/denoised.png");
    }

    @Test
    void job_생성시_완료상태와_실패상태가_아니다() {
        Job job = new Job("id-1", "test.mp4", "general", 25f, false, false);

        assertThat(job.isDone()).isFalse();
        assertThat(job.isFailed()).isFalse();
    }

    // ──────────────────────────────────────────
    // Phase 전이
    // ──────────────────────────────────────────

    @Test
    void DONE_phase일때만_isDone이_true를_반환한다() {
        Job job = new Job("id-1", "test.mp4", "general", 25f, false, false);

        for (Job.Phase phase : Job.Phase.values()) {
            job.setPhase(phase);
            assertThat(job.isDone()).isEqualTo(phase == Job.Phase.DONE);
        }
    }

    @Test
    void FAILED_phase일때만_isFailed가_true를_반환한다() {
        Job job = new Job("id-1", "test.mp4", "general", 25f, false, false);

        for (Job.Phase phase : Job.Phase.values()) {
            job.setPhase(phase);
            assertThat(job.isFailed()).isEqualTo(phase == Job.Phase.FAILED);
        }
    }

    @Test
    void isDone과_isFailed가_동시에_true인_경우는_없다() {
        Job job = new Job("id-1", "test.mp4", "general", 25f, false, false);

        for (Job.Phase phase : Job.Phase.values()) {
            job.setPhase(phase);
            assertThat(job.isDone() && job.isFailed()).isFalse();
        }
    }
}
