package dev.learning.request.model;

/**
 * Request-line components, stored without additional validation.
 *
 * @param method the method text
 * @param target the request target, including any query string
 * @param version the version text, such as {@code HTTP/1.1}
 */
public record HttpRequestLine(
        String method,
        String target,
        String version
) {
}
