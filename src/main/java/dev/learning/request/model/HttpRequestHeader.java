package dev.learning.request.model;

/**
 * A request head consisting of a request line and header fields, without a body.
 *
 * <p>Components are not validated here; the referenced field collection remains mutable.
 *
 * @param requestLine the request-line components
 * @param headerFields the shared header-field collection
 */
public record HttpRequestHeader(
        HttpRequestLine requestLine,
        HttpHeaderFields headerFields
) {
}
