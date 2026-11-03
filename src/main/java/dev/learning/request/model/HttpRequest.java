package dev.learning.request.model;

/**
 * Combines a request head with its raw body bytes.
 *
 * <p>The constructor and getters store and expose the supplied references without
 * defensive copies. The body array and the header's field collection remain mutable.
 */
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
