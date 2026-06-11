package com.nightvision.service;

import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.PrematureCloseException;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 외부 AI 추론 서버 클라이언트
 *
 * POST /denoise/video  : FastDVDnet 영상 디노이징 → mp4
 * POST /denoise/raw    : RViDeNet RAW 디노이징   → denoised PNG
 * POST /visualize/raw  : RAW 입력 시각화 (추론 없음) → noisy PNG
 */
@Component
public class InferenceClient {

    @Value("${app.inference-base-url}")
    private String inferenceBaseUrl;

    @Value("${app.inference-api-key}")
    private String inferenceApiKey;

    @Value("${app.inference.timeout-minutes:20}")
    private int timeoutMinutes;

    @Value("${app.inference.max-retries:3}")
    private int maxRetries;

    @Value("${app.inference.retry-backoff-seconds:10}")
    private int retryBackoffSeconds;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        ConnectionProvider provider = ConnectionProvider.builder("inference-pool")
                .maxConnections(10)
                .maxIdleTime(Duration.ofMinutes(30))
                .maxLifeTime(Duration.ofHours(1))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000)
                .responseTimeout(Duration.ofMinutes(timeoutMinutes));

        this.webClient = WebClient.builder()
                .baseUrl(inferenceBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs()
                        .maxInMemorySize(512 * 1024 * 1024))
                .build();
    }

    /** POST /denoise/video → denoised mp4 바이트 반환 */
    public byte[] denoise(Path inputFile, float noiseSigma, boolean addNoise, boolean compare) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(inputFile));
        body.add("noise_sigma", noiseSigma);
        body.add("add_noise", addNoise);
        body.add("compare", compare);
        return call("/denoise/video", body);
    }

    /**
     * POST /denoise/raw → denoised PNG 바이트 반환 (RViDeNet 추론 결과)
     * 모든 RAW 파라미터는 서버 기본값 사용 (height=1080, width=1920 등)
     */
    public byte[] denoiseRaw(Path inputFile) {
        return call("/denoise/raw", buildRawBody(inputFile));
    }

    /**
     * POST /visualize/raw → noisy PNG 바이트 반환 (추론 없이 입력 시각화)
     * /denoise/raw 추론 전에 먼저 호출하여 입력 이미지를 빠르게 표시
     */
    public byte[] visualizeRaw(Path inputFile) {
        return call("/visualize/raw", buildRawBody(inputFile));
    }

    /** RAW 요청 공통 body 생성 — 파라미터는 서버 기본값 사용 */
    private MultiValueMap<String, Object> buildRawBody(Path inputFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(inputFile));
        return body;
    }

    private byte[] call(String uri, MultiValueMap<String, Object> body) {
        return webClient.post()
                .uri(uri)
                .header("X-API-Key", inferenceApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(msg -> new WebClientResponseException(
                                        response.statusCode().value(),
                                        msg, response.headers().asHttpHeaders(), null, null))
                )
                .bodyToMono(byte[].class)
                .timeout(Duration.ofMinutes(timeoutMinutes))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(retryBackoffSeconds))
                        .filter(e -> {
                            if (e instanceof PrematureCloseException) return false;
                            if (e instanceof WebClientResponseException ex
                                    && ex.getStatusCode().is4xxClientError()) return false;
                            return true;
                        })
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException(
                                        "모델 서버에 연결할 수 없습니다. 서버 상태를 확인해주세요. (재시도 "
                                                + maxRetries + "회 실패)", signal.failure())))
                .block();
    }
}
