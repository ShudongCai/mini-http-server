
package dev.learning.request.parser;

import dev.learning.request.model.HttpHeaderFields;

import java.nio.charset.StandardCharsets;

public class HttpHeaderFieldsParser {

    public HttpHeaderFields parse(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Header input must not be null");
        }

        HttpHeaderFields httpHeaderFields = new HttpHeaderFields();
        int lineStart = 0;

        for (int i = 0; i < input.length; i++) {

            // Reject LF without CR
            if (input[i] == '\n') {
                throw new IllegalArgumentException("Invalid header line ending");
            }

            if (input[i] != '\r') {
                continue;
            }

            // Validate CRLF
            if (i + 1 >= input.length || input[i + 1] != '\n') {
                throw new IllegalArgumentException("Invalid header line ending");
            }

            // Empty line marks the end of the header section
            if (i == lineStart) {
                if (i + 2 != input.length) {
                    throw new IllegalArgumentException("Unexpected data after headers");
                }

                return httpHeaderFields;
            }

            // Parse the current header line
            String[] header = parseFieldLine(input, lineStart, i);

            String headerName = header[0];
            String headerValue = stripOptionalWhitespace(header[1]);

            httpHeaderFields.add(headerName, headerValue);

            // Move to the start of the next line
            lineStart = i + 2;
            i++; // Skip LF
        }

        throw new IllegalArgumentException("Missing header section terminator");
    }

    private String[] parseFieldLine(byte[] input, int start, int endExclusive) {
        String line = new String(
                input,
                start,
                endExclusive - start,
                StandardCharsets.ISO_8859_1
        );

        String[] parts = line.split(":", 2);

        if (parts.length != 2 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid HTTP header field");
        }

        return parts;
    }

    private String stripOptionalWhitespace(String value) {
        int start = 0;
        int end = value.length();

        while (start < end &&
                (value.charAt(start) == ' ' || value.charAt(start) == '\t')) {
            start++;
        }

        while (end > start &&
                (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\t')) {
            end--;
        }

        return value.substring(start, end);
    }
}
