package com.vaixle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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


public class CrptApi {

    private static final HttpClient HTTP_CLIENT;

    private final ExecutorService threadService;

    private final TimedSemaphore timedSemaphore;

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
            e.printStackTrace();
        }
        return null;
    }

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
            e.printStackTrace();
        }
        return null;
    }

    public void createDocumentGoodsProducedRussia(Document document, String signature) {
        TokenResponse tokenResponse = authenticate(signature);
        threadService.submit(() -> sendRequest(document, signature, tokenResponse));
    }

    public void sendRequest(Document document, String signature, TokenResponse tokenResponse) {
        document.setSignature(signature);
        try {
            timedSemaphore.acquire();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GlobalProperties.URL_CREATE_DOCUMENT + document.getProductGroup()))
                    .header(GlobalProperties.HEADER_AUTHORIZATION, "Bearer " + tokenResponse.getToken())
                    .header(GlobalProperties.HEADER_CONTENT_TYPE, "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(document)))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpClient configureHTTP() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(GlobalProperties.CONNECT_TIMEOUT))
                .build();
    }

    static {
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

    private static class GlobalProperties {

        private static final long PERIOD = 5;

        private static final int THREADS = 4;

        private static final int CONNECT_TIMEOUT = 5;

        private static final String URL_AUTHORIZATION = "https://ismp.crpt.ru/api/v3/auth/cert/key";

        private static final String URL_TOKEN = "https://ismp.crpt.ru/api/v3/auth/cert/";

        private static final String URL_CREATE_DOCUMENT = "https://ismp.crpt.ru/api/v3/lk/documents/create?pg=";

        private static final String HEADER_AUTHORIZATION = "Authorization";

        private static final String HEADER_CONTENT_TYPE = "content-type";

    }

    private static class KeyResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        private String uuid;

        private String data;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    private static class TokenResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

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

        public Document() {
        }

        public Document(String documentFormat, String productDocument, String productGroup, String signature, String type) {
            this.documentFormat = documentFormat;
            this.productDocument = productDocument;
            this.productGroup = productGroup;
            this.signature = signature;
            this.type = type;
        }

        public String getDocumentFormat() {
            return documentFormat;
        }

        public void setDocumentFormat(String documentFormat) {
            this.documentFormat = documentFormat;
        }

        public String getProductDocument() {
            return productDocument;
        }

        public void setProductDocument(String productDocument) {
            this.productDocument = productDocument;
        }

        public String getProductGroup() {
            return productGroup;
        }

        public void setProductGroup(String productGroup) {
            this.productGroup = productGroup;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
