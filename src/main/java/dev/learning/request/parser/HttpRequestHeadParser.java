package dev.learning.request.parser;

import dev.learning.request.model.HttpRequest;
import dev.learning.request.model.HttpRequestLine;
import dev.learning.request.model.HttpHeaderFields;

import java.util.Arrays;

public class HttpRequestHeadParser {
    public HttpRequest parse(byte[] input) {
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

            return new HttpRequest(requestLine, headerFields);
        }

        throw new IllegalArgumentException("The http header is invalid");
    }
}
