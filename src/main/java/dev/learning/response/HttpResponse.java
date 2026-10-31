package dev.learning.response;

import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private final String content;

    private HttpResponse(String content) {
        if (content == null) {
            content = "";
        }
        this.content = content;
    }

    public String toHttpString() {
        String length = String.valueOf(content.getBytes(StandardCharsets.UTF_8).length);
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + length + "\r\n" +
                """
                Connection: close\r
                \r
                """
                + content;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public static class HttpResponseBuilder {
        private String content;

        public HttpResponseBuilder setContent(String content) {
            if (content == null) {
                content = "";
            }

            this.content = content;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(content);
        }
    }
}
