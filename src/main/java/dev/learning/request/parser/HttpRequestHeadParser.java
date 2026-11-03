package dev.learning.request.parser;
import dev.learning.request.model.HttpRequestHeader;
import dev.learning.request.model.HttpRequestLine;
import dev.learning.request.model.HttpHeaderFields;

import java.util.Arrays;

/**
 * Parses a complete request head by delegating request-line and header-field parsing.
 *
 * <p>This parser reads no stream, retains no request state and does not process the body.
 */
public class HttpRequestHeadParser {
    /**
     * Parses one complete head, excluding the request body and any subsequent request.
     *
     * @param input the request line and header fields, including the final empty CRLF line
     * @return a new request header containing the parsed line and fields
     * @throws IllegalArgumentException if the input is null, empty or rejected by either
     *         component parser, including missing terminators or trailing data
     */
    public HttpRequestHeader parse(byte[] input) {
        if (input == null || input.length < 1) {
            throw new IllegalArgumentException("The http header is invalid");
        }

        for (int i = 0; i < input.length; ++i) {
            if (input[i] == '\n') {
                throw new IllegalArgumentException("The http header is invalid");
            }

            if (input[i] != '\r') {
                continue;
            }

            if (i + 1 >= input.length || input[i + 1] != '\n') {
                throw new IllegalArgumentException("The Http Header is Invalid");
            }

            HttpRequestLineParser requestLineParser = new HttpRequestLineParser();
            HttpHeaderFieldsParser headerFieldsParser = new HttpHeaderFieldsParser();

            HttpRequestLine requestLine =
                    requestLineParser.parse(
                            Arrays.copyOfRange(input, 0, i + 2)
                    );

            HttpHeaderFields headerFields =
                    headerFieldsParser.parse(
                            Arrays.copyOfRange(input, i + 2, input.length)
                    );

            return new HttpRequestHeader(requestLine, headerFields);
        }

        throw new IllegalArgumentException("The http header is invalid");
    }
}
