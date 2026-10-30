package dev.learning;

import java.nio.charset.StandardCharsets;

public class HttpRequestLineParser {

    public HttpRequestLine parseHttpRequestLine(byte[] input) {
        int position = 0;
        int dataIndex = 0;
        String method = "";
        String target = "";
        String version = "";
        for (int i = 0; i < input.length; ++i) {
            if (input[i] == ' ') {
                if (dataIndex == 0 && input[position] != ' ') {
                    method = new String(
                            input,
                            position,
                            i - position,
                            StandardCharsets.UTF_8
                    );
                    dataIndex++;
                    position = i + 1;
                } else if (dataIndex == 1 && input[position] != ' ') {
                    target = new String(
                            input,
                            position,
                            i - position,
                            StandardCharsets.UTF_8
                    );
                    dataIndex++;
                    position = i + 1;
                } else {
                    throw new IllegalArgumentException("Invalid request spacing");
                }
            }

            if (input[i] == '\r') {
                if (dataIndex != 2 || i == position) {
                    throw new IllegalArgumentException("Invalid request line");
                }

                if (i + 1 >= input.length || input[i + 1] != '\n') {
                    throw new IllegalArgumentException("Invalid request line ending");
                }

                version = new String(
                        input,
                        position,
                        i - position,
                        StandardCharsets.UTF_8
                );
                return new HttpRequestLine(method, target, version);
            }
        }

        throw new IllegalArgumentException("Missing request line ending");
    }
}
