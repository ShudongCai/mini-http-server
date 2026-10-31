package dev.learning;

import java.util.*;

public class HttpHeaders {
    private final Map<String, List<String>> headers;

    public HttpHeaders() {
        headers = new HashMap<>();
    }

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

    public List<String> get(String name) {
        Objects.requireNonNull(name, "Header name must not be null");
        String normalizedName = name.toUpperCase(Locale.ROOT);
        return List.copyOf(headers.getOrDefault(normalizedName, List.of()));
    }
}
