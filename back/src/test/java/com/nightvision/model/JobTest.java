package com.nightvision.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobTest {

    @Test
    void job생성시_초기상태가_QUEUED로_설정된다() {
        Job job = new Job("id-1", "test.mp4", 25f, false, false);

        assertThat(job.getJobId()).isEqualTo("id-1");
        assertThat(job.getOriginalFileName()).isEqualTo("test.mp4");
        assertThat(job.getNoiseSigma()).isEqualTo(25f);
        assertThat(job.isAddNoise()).isFalse();
        assertThat(job.isCompare()).isFalse();
        assertThat(job.getPhase()).isEqualTo(Job.Phase.QUEUED);
        assertThat(job.getPercent()).isEqualTo(0);
        assertThat(job.getCreatedAt()).isNotNull();
    }

    @Test
    void job생성시_완료상태와_실패상태가_아니다() {
        Job job = new Job("id-1", "test.mp4", 25f, false, false);

        assertThat(job.isDone()).isFalse();
        assertThat(job.isFailed()).isFalse();
    }

    @Test
    void DONE_phase일때만_isDone이_true를_반환한다() {
        Job job = new Job("id-1", "test.mp4", 25f, false, false);

        for (Job.Phase phase : Job.Phase.values()) {
            job.setPhase(phase);
            assertThat(job.isDone()).isEqualTo(phase == Job.Phase.DONE);
        }
    }

    @Test
    void FAILED_phase일때만_isFailed가_true를_반환한다() {
        Job job = new Job("id-1", "test.mp4", 25f, false, false);

        for (Job.Phase phase : Job.Phase.values()) {
            job.setPhase(phase);
            assertThat(job.isFailed()).isEqualTo(phase == Job.Phase.FAILED);
        }
    }

    @Test
    void isDone과_isFailed가_동시에_true인_경우는_없다() {
        Job job = new Job("id-1", "test.mp4", 25f, false, false);

        for (Job.Phase phase : Job.Phase.values()) {
            job.setPhase(phase);
            assertThat(job.isDone() && job.isFailed()).isFalse();
        }
    }
}
