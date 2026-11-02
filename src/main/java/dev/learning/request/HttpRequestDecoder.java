package dev.learning.request;

import dev.learning.request.model.HttpRequest;
import dev.learning.request.model.HttpRequestHeader;
import dev.learning.request.parser.HttpRequestHeadParser;
import dev.learning.request.reader.HttpRequestReader;

import java.io.IOException;
import java.io.InputStream;

public class HttpRequestDecoder {
    private static final int MAX_BODY_SIZE = 1024 * 1024;

    private final HttpRequestReader httpRequestReader;
    private final HttpRequestHeadParser httpRequestHeadParser;

    public HttpRequestDecoder() {
        this.httpRequestReader = new HttpRequestReader();
        this.httpRequestHeadParser = new HttpRequestHeadParser();
    }

    public HttpRequest decode(InputStream stream) throws IOException {
        byte[] rawHeader = httpRequestReader.readHead(stream);

        if (rawHeader == null) {
            return null;
        }

        HttpRequestHeader header = httpRequestHeadParser.parse(rawHeader);

        byte[] body = getBody(stream, header);

        return new HttpRequest(header, body);
    }

    private byte[] getBody(InputStream stream, HttpRequestHeader header) throws IOException{
        if (!header.headerFields().get("Transfer-Encoding").isEmpty() &&
                !header.headerFields().get("Content-Length").isEmpty()) {
            throw new IllegalArgumentException("The Header contains both Content-Length and Transfer-Encoding");
        }

        if (header.headerFields().get("Transfer-Encoding").size() > 1) {
            throw new IllegalArgumentException("Transfer-Encoding has multiply values");
        }

        if (header.headerFields().get("Transfer-Encoding").size() == 1) {
            return readBodyWithTransferEncoding(stream, header);
        }

        if (header.headerFields().get("Content-Length").size() > 1) {
            throw new IllegalArgumentException("Body content length has multiply values");
        }

        if (header.headerFields().get("Content-Length").size() == 1) {
            return readBodyWithContentLength(stream, header);
        }

        return new byte[0];
    }

    private byte[] readBodyWithTransferEncoding(InputStream stream, HttpRequestHeader header) throws IOException {
        throw new UnsupportedOperationException("Does not support Transfer Encoding yet");
    }

    private byte[] readBodyWithContentLength(InputStream stream, HttpRequestHeader header) throws IOException {
        String contentLength = header.headerFields().get("Content-Length").getFirst();

        if (!isNumeric(contentLength)) {
            throw new IllegalArgumentException("Body content length is not a number");
        }

        long length = Long.parseLong(contentLength);

        if (length > MAX_BODY_SIZE) {
            throw new IllegalArgumentException("HTTP request body too large");
        }

        return httpRequestReader.readExactly(stream, (int) length);
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }

        for (char ch : str.toCharArray()) {
            if (ch < '0' || ch > '9') {
                return false;
            }
        }

        return true;
    }
}
