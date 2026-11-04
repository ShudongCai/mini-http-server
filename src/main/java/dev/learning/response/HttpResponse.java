package dev.learning.response;

import java.nio.charset.StandardCharsets;

/**
 * An immutable HTTP/1.1 response with a text body and fixed header values.
 *
 * <p>Serialization uses a fixed text/plain content type with UTF-8 charset and a
 * Content-Length measured in UTF-8 bytes. Built instances can be shared across
 * threads. The demo server creates a separate response with a connection-specific
 * Connection value before sending, leaving the handler's response unchanged.
 * This class does not manage socket lifetimes.
 */
public class HttpResponse {
    private final int statusCode;
    private final String reasonPhrase;
    private final String content;
    private final String connection;

    private HttpResponse(int statusCode, String reasonPhrase, String content, String connection) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.content = content;
        this.connection = connection;
    }

    /**
     * Serializes the status line, headers, empty line and text body to a string.
     *
     * <p>Encode the returned string as UTF-8 when sending it so the body length matches
     * Content-Length. This method does not write to a stream.
     *
     * @return the complete HTTP/1.1 text representation
     */
    public String toHttpString() {
        String length = String.valueOf(content.getBytes(StandardCharsets.UTF_8).length);
        return "HTTP/1.1 "+ statusCode + " " + reasonPhrase+ "\r\n" +
                "Content-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: " + connection + "\r\n" +
                "\r\n"
                + content;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public String getContent() {
        return content;
    }

    public String getConnection() {
        return connection;
    }

    public static HttpResponseBuilder builder() {
        return new HttpResponseBuilder();
    }

    /**
     * Builds responses defaulting to 200 OK, an empty body and keep-alive.
     *
     * <p>Status code and reason phrase are independent: changing the code does not
     * automatically update the phrase.
     * The builder is mutable and not thread-safe; its settings are copied into
     * a new immutable response by {@link #build()}.
     */
    public static class HttpResponseBuilder {
        private int statusCode = 200;
        private String reasonPhrase = "OK";
        private String content = "";
        private String connection = "keep-alive";

        /**
         * Sets the text body, treating null as an empty string.
         *
         * @param content the body text, or null for an empty body
         * @return this builder
         */
        public HttpResponseBuilder setContent(String content) {
            if (content == null) {
                content = "";
            }

            this.content = content;
            return this;
        }

        /**
         * Sets the status code without changing the reason phrase.
         *
         * @param statusCode a code from 100 through 599
         * @return this builder
         * @throws IllegalArgumentException if the code is outside that range
         */
        public HttpResponseBuilder setStatusCode(int statusCode) {
            if (statusCode < 100 || statusCode > 599) {
                throw new IllegalArgumentException("Invalid HTTP status code");
            }
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Sets the reason phrase, treating null as UNKNOWN.
         *
         * @param reasonPhrase the phrase to use, or null for UNKNOWN
         * @return this builder
         */
        public HttpResponseBuilder setReasonPhrase(String reasonPhrase) {
            if (reasonPhrase == null) {
                reasonPhrase = "UNKNOWN";
            }
            this.reasonPhrase = reasonPhrase;
            return this;
        }

        /**
         * Sets Connection, defaulting null to keep-alive.
         *
         * <p>Comma-separated tokens are trimmed for comparison. If any token equals
         * close ignoring case, the stored value is close; otherwise the supplied
         * text is retained.
         *
         * @param connection the header value, or null for keep-alive
         * @return this builder
         */
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
                if (value.trim().equalsIgnoreCase("close")) {
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
