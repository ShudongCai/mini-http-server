package dev.learning.response;

import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private final int statusCode;
    private final String reasonPhrase;
    private final String content;

    private HttpResponse(int statusCode, String reasonPhrase, String content) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.content = content;
    }

    public String toHttpString() {
        String length = String.valueOf(content.getBytes(StandardCharsets.UTF_8).length);
        return "HTTP/1.1 "+ statusCode + " " + reasonPhrase+ "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n"
                + content;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public static class HttpResponseBuilder {
        private int statusCode = 200;
        private String reasonPhrase = "OK";
        private String content = "";

        public HttpResponseBuilder setContent(String content) {
            if (content == null) {
                content = "";
            }

            this.content = content;
            return this;
        }

        public HttpResponseBuilder setStatusCode(int statusCode) {
            if (statusCode < 100 || statusCode > 599) {
                throw new IllegalArgumentException("Invalid HTTP status code");
            }
            this.statusCode = statusCode;
            return this;
        }

        public HttpResponseBuilder setReasonPhrase(String reasonPhrase) {
            if (reasonPhrase == null) {
                reasonPhrase = "UNKNOWN";
            }
            this.reasonPhrase = reasonPhrase;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(statusCode, reasonPhrase, content);
        }
    }
}
