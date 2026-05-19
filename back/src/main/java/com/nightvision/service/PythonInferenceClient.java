package com.nightvision.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

/**
 * Python 추론 서버(FastAPI 등)와 통신하는 클라이언트.
 *
 * Python 서버가 기대하는 엔드포인트:
 *   POST /infer
 *   Form: file (binary), mode (string), intensity (string)
 *   Response: 처리된 파일 바이너리
 */
@Component
public class PythonInferenceClient {

    @Value("${app.python-server-url}")
    private String pythonServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] infer(Path inputFile, String mode, String intensity) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(inputFile));
        body.add("mode", mode);
        body.add("intensity", intensity);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                pythonServerUrl + "/infer",
                HttpMethod.POST,
                request,
                byte[].class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Python 서버에서 빈 응답을 반환했습니다.");
        }

        return response.getBody();
    }
}
