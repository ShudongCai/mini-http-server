package dev.learning.router.model;

/**
 * A case-sensitive method and path pair used for exact route lookup.
 *
 * <p>No validation or normalization is performed by this record.
 *
 * @param method the method text used as part of the key
 * @param path the path text used as part of the key
 */
public record RouteKey(
        String method,
        String path
) {
}
