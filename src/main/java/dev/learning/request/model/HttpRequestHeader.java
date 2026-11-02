package dev.learning.request.model;

public record HttpRequestHeader(
        HttpRequestLine requestLine,
        HttpHeaderFields headerFields
) {
}
