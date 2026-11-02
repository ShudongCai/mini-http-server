package dev.learning.response;

import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private final int statusCode;
    private final String reasonPhrase;
    private final String content;
    private String connection;

    private HttpResponse(int statusCode, String reasonPhrase, String content, String connection) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.content = content;
        this.connection = connection;
    }

    public String toHttpString() {
        String length = String.valueOf(content.getBytes(StandardCharsets.UTF_8).length);
        return "HTTP/1.1 "+ statusCode + " " + reasonPhrase+ "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: " + connection + "\r\n" +
                "\r\n"
                + content;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    public static class HttpResponseBuilder {
        private int statusCode = 200;
        private String reasonPhrase = "OK";
        private String content = "";
        private String connection = "keep-alive";

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

        public HttpResponseBuilder setConnection(String connection) {
            if (connection == null ) {
                connection = "keep-alive";
            }

            if (hasCloseOption(connection)) {
                connection = "close";
            }

            this.connection = connection;
            return this;
        }

        private boolean hasCloseOption(String connection) {
            String[] connectionValues = connection.split(",");
            for (String value : connectionValues) {
                if (value.equalsIgnoreCase("close")) {
                    return true;
                }
            }

            return false;
        }

        public HttpResponse build() {
            return new HttpResponse(
                    statusCode,
                    reasonPhrase,
                    content,
                    connection
            );
        }
    }
}
