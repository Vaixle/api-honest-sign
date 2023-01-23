package com.vaixle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.TimedSemaphore;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Getter
@Setter
public class CrptApi {

    static HttpClient HTTP_CLIENT;

    ExecutorService threadService;

    TimedSemaphore timedSemaphore;

    public CrptApi() {
        this(TimeUnit.SECONDS, 100);
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        threadService = Executors.newFixedThreadPool(GlobalProperties.THREADS);
        timedSemaphore = new TimedSemaphore(GlobalProperties.PERIOD, timeUnit, requestLimit);
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 4);
    }
    /**
     * Before sending request we have to pass authentication
     * There are two steps.
     * 1) Get key
     * 2) Then we have to swap our key on token
     * We have to use signature to sign our data
     * TODO: https://markirovka.demo.crpt.tech/ doesn't work, i can't get signature
     */
    private static TokenResponse authenticate(String signature) {
        KeyResponse keyResponse = getKey();
        if (Objects.isNull(keyResponse))
            throw new GetKeyException();
        keyResponse.setData(Base64.getEncoder().encodeToString((keyResponse.getData() + signature).getBytes()));
        TokenResponse tokenResponse = getToken(keyResponse);
        if (Objects.isNull(tokenResponse))
            throw new GetTokenException();
        return tokenResponse;
    }

    /**
     * Send request to get key
     */
    private static KeyResponse getKey() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GlobalProperties.URL_AUTHORIZATION))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(response.body(), KeyResponse.class);
        } catch (InterruptedException | IOException e) {
            log.error("Error get key", e);
        }
        return null;
    }

    /**
     * Send request to swap our key on token
     */
    private static TokenResponse getToken(KeyResponse keyResponse) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GlobalProperties.URL_TOKEN))
                    .header(GlobalProperties.HEADER_CONTENT_TYPE, "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(keyResponse)))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(response.body(), TokenResponse.class);
        } catch (InterruptedException | IOException e) {
            log.error("Error get token", e);
        }
        return null;
    }

    /**
     * A unified method of creating documents
     */
    public void createDocumentGoodsProducedRussia(Document document, String signature) {
        TokenResponse tokenResponse = authenticate(signature);
        document.setSignature(signature);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GlobalProperties.URL_CREATE_DOCUMENT + document.getProductGroup()))
                    .header(GlobalProperties.HEADER_AUTHORIZATION, "Bearer " + tokenResponse.getToken())
                    .header(GlobalProperties.HEADER_CONTENT_TYPE, "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(document)))
                    .build();
            threadService.submit(() -> sendRequest(request));
        } catch (JsonProcessingException e) {
            log.error("Error parse object as JSON", e);
        }
    }

    public HttpResponse<String> sendRequest(HttpRequest request) {
        log.info("Sending request");
        try {
            timedSemaphore.acquire();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            log.error("Error get response", e);
        }
        return null;
    }

    private static HttpClient configureHTTP() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(GlobalProperties.CONNECT_TIMEOUT))
                .build();
    }

    static {
        log.info("create HTTP client");
        HTTP_CLIENT = configureHTTP();
    }

    public static class GetKeyException extends RuntimeException implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final String DEFAULT_MESSAGE = "Authorization error get key!";

        public GetKeyException() {
            super(DEFAULT_MESSAGE);
        }

        public GetKeyException(String message) {
            super(message);
        }

        public GetKeyException(String message, Throwable e) {
            super(message, e);
        }
    }

    public static class GetTokenException extends RuntimeException implements Serializable {

        private static final long serialVersionUID = 1L;
        private static final String DEFAULT_MESSAGE = "Authorization error get token!";

        public GetTokenException() {
            super(DEFAULT_MESSAGE);
        }

        public GetTokenException(String message) {
            super(message);
        }

        public GetTokenException(String message, Throwable e) {
            super(message, e);
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class GlobalProperties {

        static long PERIOD = 5;

        static int THREADS = 4;

        static int CONNECT_TIMEOUT = 5;

        static String URL_AUTHORIZATION = "https://ismp.crpt.ru/api/v3/auth/cert/key";

        static String URL_TOKEN = "https://ismp.crpt.ru/api/v3/auth/cert/";

        static String URL_CREATE_DOCUMENT = "https://ismp.crpt.ru/api/v3/lk/documents/create?pg=";

        static String HEADER_AUTHORIZATION = "Authorization";

        static String HEADER_CONTENT_TYPE = "content-type";

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    private static class KeyResponse implements Serializable {

        static final long serialVersionUID = 1L;

        String uuid;

        String data;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    private static class TokenResponse implements Serializable {

        static final long serialVersionUID = 1L;

        String token;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Document implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("document_format")
        private String documentFormat;

        @JsonProperty("product_document")
        private String productDocument;

        @JsonProperty("product_group")
        private String productGroup;

        private String signature;

        private String type;
    }
}
