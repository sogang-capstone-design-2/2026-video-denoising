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

    private String originalFileName;

    @Column(nullable = false, columnDefinition = "float default 25")
    private float noiseSigma = 25f;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean addNoise = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean compare = false;

    @Enumerated(EnumType.STRING)
    private volatile Phase phase = Phase.QUEUED;

    private volatile int percent = 0;
    private volatile String inputPath;   // 원본 파일 경로 (before)
    private volatile String resultPath;  // 처리 결과 경로 (after)
    private volatile String errorMessage;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Job(String jobId, String originalFileName, float noiseSigma, boolean addNoise, boolean compare) {
        this.jobId = jobId;
        this.originalFileName = originalFileName;
        this.noiseSigma = noiseSigma;
        this.addNoise = addNoise;
        this.compare = compare;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isDone() {
        return phase == Phase.DONE;
    }

    public boolean isFailed() {
        return phase == Phase.FAILED;
    }
}
