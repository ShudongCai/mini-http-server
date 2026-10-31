package dev.learning.request;

import dev.learning.request.headerFields.HttpHeaderFields;
import dev.learning.request.headerRequest.HttpRequestLine;

public class HttpRequest {
    private final HttpRequestLine httpRequestLine;
    private final HttpHeaderFields httpHeaderFields;

    public HttpRequest(HttpRequestLine httpRequestLine, HttpHeaderFields httpHeaderFields) {
        this.httpRequestLine = httpRequestLine;
        this.httpHeaderFields = httpHeaderFields;
    }

    public HttpRequestLine getHttpRequestLine() {
        return httpRequestLine;
    }

    public HttpHeaderFields getHttpHeaderFields() {
        return httpHeaderFields;
    }
}
