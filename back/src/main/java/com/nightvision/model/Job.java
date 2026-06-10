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

    /** "general" (영상/FastDVDnet) | "lowlight" (RAW/RViDeNet) */
    @Column(nullable = false, columnDefinition = "varchar(20) default 'general'")
    private String mode = "general";

    private String originalFileName;

    // ── general 모드 파라미터 ──────────────────────────────────────
    @Column(nullable = false, columnDefinition = "float default 25")
    private float noiseSigma = 25f;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean addNoise = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean compare = false;

    // ── 공통 상태 ─────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private volatile Phase phase = Phase.QUEUED;

    private volatile int percent = 0;

    /** 원본 업로드 파일 경로 (video: mp4 / RAW: .raw) */
    private volatile String inputPath;

    /**
     * lowlight 전용: 추론 결과 ZIP에서 추출한 noisy.png 경로.
     * /files/before 요청 시 inputPath 대신 이 경로를 우선 사용.
     */
    private volatile String noisyImagePath;

    /** 처리 결과 경로 (video: denoised mp4 / RAW: denoised.png) */
    private volatile String resultPath;

    private volatile String errorMessage;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Job(String jobId, String originalFileName, String mode,
               float noiseSigma, boolean addNoise, boolean compare) {
        this.jobId = jobId;
        this.originalFileName = originalFileName;
        this.mode = mode;
        this.noiseSigma = noiseSigma;
        this.addNoise = addNoise;
        this.compare = compare;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isDone()   { return phase == Phase.DONE; }
    public boolean isFailed() { return phase == Phase.FAILED; }
}
