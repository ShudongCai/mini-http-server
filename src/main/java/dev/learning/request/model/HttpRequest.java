package dev.learning.request.model;

public class HttpRequest {
    private final HttpRequestHeader httpRequestHeader;
    private final byte[] httpBody;

    public HttpRequest(HttpRequestHeader httpRequestHeader, byte[] httpBody) {
        this.httpRequestHeader = httpRequestHeader;
        this.httpBody = httpBody;
    }

    public HttpRequestHeader getHttpRequestHeader() {
        return httpRequestHeader;
    }

    public byte[] getHttpBody() {
        return httpBody;
    }
}
