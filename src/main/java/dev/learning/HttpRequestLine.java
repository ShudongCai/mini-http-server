package dev.learning;

public record HttpRequestLine(
        String method,
        String target,
        String version
) {
}
