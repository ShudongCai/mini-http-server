package dev.learning.request.model;

/**
 * Common HTTP method names; not an exhaustive set of valid methods.
 *
 * <p>Request parsing and routing currently use method strings rather than this enum.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
}
