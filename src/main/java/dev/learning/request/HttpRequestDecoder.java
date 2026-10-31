package dev.learning.request;

import dev.learning.request.parser.HttpRequestHeadParser;
import dev.learning.request.reader.HttpRequestReader;

public class HttpRequestDecoder {
    private final HttpRequestReader httpRequestReader;
    private final HttpRequestHeadParser httpRequestHeadParser;

    public HttpRequestDecoder() {
        this.httpRequestReader = new HttpRequestReader();
        this.httpRequestHeadParser = new HttpRequestHeadParser();
    }


}
