package com.nightvision.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.nio.file.Path;
import java.time.Duration;

/**
 * S2.2 — Python 추론 서버 클라이언트
 * WebClient 기반, 타임아웃·재시도 정책 포함
 */
@Component
public class InferenceClient {

    @Value("${app.python-server-url}")
    private String pythonServerUrl;

    @Value("${app.inference.timeout-minutes:10}")
    private int timeoutMinutes;

    @Value("${app.inference.max-retries:2}")
    private int maxRetries;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(pythonServerUrl)
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(512 * 1024 * 1024)) // 영상이기 때문에 512MB
                .build();
    }

    /**
     * POST /infer 호출 → 처리된 파일 바이트 반환
     * - 타임아웃: app.inference.timeout-minutes (기본 10분)
     * - 재시도: app.inference.max-retries (기본 2회, 3초 백오프)
     *   단, 4xx 클라이언트 오류는 재시도하지 않음
     */
    public byte[] infer(Path inputFile, String mode, String intensity) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(inputFile));
        body.add("mode", mode);
        body.add("intensity", intensity);

        return webClient.post()
                .uri("/infer")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .map(msg -> new RuntimeException("추론 서버 요청 오류 (4xx): " + msg))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(msg -> new RuntimeException("추론 서버 내부 오류 (5xx): " + msg))
                )
                .bodyToMono(byte[].class)
                .timeout(Duration.ofMinutes(timeoutMinutes))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(3))
                        .filter(e -> !(e instanceof WebClientResponseException ex)
                                || ex.getStatusCode().is5xxServerError()))
                .block();
    }
}
