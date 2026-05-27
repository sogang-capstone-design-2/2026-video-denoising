package com.nightvision.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Job {

    /**
     * QUEUED ──▶ UPLOADING ──▶ PROCESSING ──▶ FINALIZING ──▶ DONE
     *                               │
     *                               └─────▶ FAILED
     */
    public enum Phase {
        QUEUED, UPLOADING, PROCESSING, FINALIZING, DONE, FAILED
    }

    @Id
    private String jobId;

    private String mode;
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    private volatile Phase phase = Phase.QUEUED;

    private volatile int percent = 0;
    private volatile String resultPath;
    private volatile String errorMessage;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Job(String jobId, String mode, String originalFileName) {
        this.jobId = jobId;
        this.mode = mode;
        this.originalFileName = originalFileName;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isDone() {
        return phase == Phase.DONE;
    }

    public boolean isFailed() {
        return phase == Phase.FAILED;
    }
}
