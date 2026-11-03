
package dev.learning.request.parser;

import dev.learning.request.model.HttpHeaderFields;

import java.nio.charset.StandardCharsets;

/**
 * Parses header fields from bytes without reading a stream or retaining request state.
 */
public class HttpHeaderFieldsParser {

    /**
     * Parses a header-field section ending with an empty CRLF line.
     *
     * <p>The input excludes the request line and body; nothing may follow the final
     * empty line. Fields are decoded with ISO-8859-1 and validated by
     * {@link HttpHeaderFields#add(String, String)}. Leading and trailing spaces and
     * tabs are removed from values, while repeated fields retain their separate values.
     *
     * @param input CRLF-terminated field lines followed by the terminating empty line
     * @return a new field collection; an input consisting only of CRLF produces an empty collection
     * @throws IllegalArgumentException if the input is null, malformed, has invalid fields,
     *         lacks the terminating empty line or contains trailing data
     */
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
