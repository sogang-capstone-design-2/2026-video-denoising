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
 * - POST /denoise      : FastDVDnet 영상 디노이징 → mp4
 * - POST /denoise/raw  : RViDeNet RAW 디노이징   → ZIP (noisy.png + denoised.png)
 *
 * FileSystemResource 스트리밍으로 파일을 전송합니다.
 */
@Component
public class InferenceClient {

    @Value("${app.inference-base-url}")
    private String inferenceBaseUrl;

    @Value("${app.inference-api-key}")
    private String inferenceApiKey;

    @Value("${app.inference.timeout-minutes:20}")
    private int timeoutMinutes;

    @Value("${app.inference.max-retries:2}")
    private int maxRetries;

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

    /** POST /denoise → denoised mp4 바이트 반환 */
    public byte[] denoise(Path inputFile, float noiseSigma, boolean addNoise, boolean compare) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(inputFile));
        body.add("noise_sigma", noiseSigma);
        body.add("add_noise", addNoise);
        body.add("compare", compare);
        return call("/denoise", body);
    }

    /** POST /denoise/raw → ZIP 바이트 반환 (noisy.png + denoised.png) */
    public byte[] denoiseRaw(Path inputFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(inputFile));
        return call("/denoise/raw", body);
    }

    private byte[] call(String uri, MultiValueMap<String, Object> body) {
        return webClient.post()
                .uri(uri)
                .header("X-API-Key", inferenceApiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                // 4xx/5xx 모두 WebClientResponseException으로 던져야 retry 필터가 정확히 동작함
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(msg -> new WebClientResponseException(
                                        response.statusCode().value(),
                                        msg, response.headers().asHttpHeaders(), null, null))
                )
                .bodyToMono(byte[].class)
                .timeout(Duration.ofMinutes(timeoutMinutes))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(3))
                        .filter(e -> {
                            // PrematureCloseException: 서버가 업로드 중 연결을 닫음 → 재시도 불필요
                            if (e instanceof PrematureCloseException) return false;
                            // 4xx 클라이언트 오류(401 포함) → 재시도 불필요
                            if (e instanceof WebClientResponseException ex
                                    && ex.getStatusCode().is4xxClientError()) return false;
                            // 5xx 서버 오류 / 네트워크 오류 → 재시도
                            return true;
                        })
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException(
                                        "모델 서버에 연결할 수 없습니다. 서버 상태를 확인해주세요. (재시도 "
                                                + maxRetries + "회 실패)", signal.failure())))
                .block();
    }
}
