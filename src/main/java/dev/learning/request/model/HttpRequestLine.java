package dev.learning.request.model;

public record HttpRequestLine(
        String method,
        String target,
        String version
) {
}
