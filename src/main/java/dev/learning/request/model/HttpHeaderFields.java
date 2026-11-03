package dev.learning.request.model;

import java.util.*;

/**
 * Mutable header fields with case-insensitive name lookup and separate repeated values.
 *
 * <p>Entries inserted through {@link #add(String, String)} use uppercase names.
 * This class provides no synchronization.
 */
public class HttpHeaderFields {
    private final Map<String, List<String>> headers;

    public HttpHeaderFields() {
        headers = new HashMap<>();
    }

    /**
     * Appends a value after validating the field name and value.
     *
     * <p>Names must be non-empty ASCII tokens. Values may contain printable ASCII
     * characters and horizontal tabs, including an empty value; whitespace is not trimmed.
     *
     * @param name the case-insensitive field name
     * @param value the value to append
     * @throws IllegalArgumentException if either argument is null or fails validation
     */
    public void add(String name, String value) {
        if (!isValidHeaderName(name)) {
            throw new IllegalArgumentException("Illegal http headers");
        }

        if (!isValidHeaderValue(value)) {
            throw new IllegalArgumentException("Header value is invalid");
        }

        String normalizedName = name.toUpperCase(Locale.ROOT);

        headers.computeIfAbsent(normalizedName, key -> new ArrayList<>()).add(value);
    }

    private boolean isValidHeaderName(String name) {
        return name != null &&
                name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+");
    }

    private boolean isValidHeaderValue(String value) {
        if (value == null) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '\t' || (c >= 0x20 && c <= 0x7E)) {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * Returns an unmodifiable snapshot of the values for a field name.
     *
     * @param name the case-insensitive field name
     * @return the values in insertion order, or an empty list if the field is absent
     * @throws NullPointerException if {@code name} is null
     */
    public List<String> get(String name) {
        Objects.requireNonNull(name, "Header name must not be null");
        String normalizedName = name.toUpperCase(Locale.ROOT);
        return List.copyOf(headers.getOrDefault(normalizedName, List.of()));
    }

    /**
     * Exposes the backing map and its mutable value lists without copying.
     *
     * <p>Changes through this map affect the collection and bypass the validation
     * and name normalization performed by {@link #add(String, String)}.
     *
     * @return the live, mutable backing map
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
