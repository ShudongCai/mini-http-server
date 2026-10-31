package dev.learning.request.parser;

import dev.learning.request.model.HttpRequestLine;

import java.nio.charset.StandardCharsets;

public class HttpRequestLineParser {

    public HttpRequestLine parse(byte[] input) {
        int methodPosition = -1;
        int targetPosition = -1;
        int versionPosition = -1;

        for (int i = 0; i < input.length; ++i) {
            if (input[i] == ' ') {
                if (methodPosition == -1) {
                    methodPosition = i;
                } else if (targetPosition == -1){
                    targetPosition = i;
                } else {
                    throw new IllegalArgumentException("Invalid HTTP header");
                }
            }

            if (input[i] == '\r') {
                if (i + 1 >= input.length || input[i + 1] != '\n') {
                    throw new IllegalArgumentException("Invalid HTTP header");
                }
                versionPosition = i;
                break;
            }

            if (input[i] == '\n') {
                throw new IllegalArgumentException("Invalid HTTP header");
            }
        }

        if (methodPosition > 0 &&
                targetPosition > methodPosition + 1 &&
                versionPosition > targetPosition + 1
        ) {
            return new HttpRequestLine(
                    extract(input, 0, methodPosition),
                    extract(input, methodPosition + 1, targetPosition),
                    extract(input, targetPosition + 1, versionPosition)
            );
        }

        throw new IllegalArgumentException("Missing request line ending");
    }

    private String extract(
            byte[] input,
            int start,
            int endExclusive
    ) {
        return new String(
                input,
                start,
                endExclusive - start,
                StandardCharsets.US_ASCII
        );
    }
}
