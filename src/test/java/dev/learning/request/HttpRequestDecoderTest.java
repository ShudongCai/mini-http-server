package dev.learning.request;

import dev.learning.request.model.HttpRequest;
import dev.learning.request.model.HttpRequestHeader;
import dev.learning.request.model.HttpRequestLine;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestDecoderTest {
    @Test
    void shouldReturnNullAtCleanEndOfStream() throws IOException {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertNull(decoder.decode(stream("")));
    }

    @Test
    void shouldDecodeHeaderWithEmptyBodyWhenNoLengthIsSpecified() throws IOException {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        HttpRequest request = decoder.decode(stream(
                "GET /products HTTP/1.1\r\nHost: localhost\r\n\r\n"
        ));

        assertNotNull(request);
        HttpRequestHeader header = request.getHttpRequestHeader();
        assertEquals(new HttpRequestLine("GET", "/products", "HTTP/1.1"), header.requestLine());
        assertEquals(List.of("localhost"), header.headerFields().get("Host"));
        assertArrayEquals(new byte[0], request.getHttpBody());
    }

    @Test
    void shouldDecodeContentLengthInBytesFromFragmentedInput() throws IOException {
        byte[] expectedBody = "你好\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        String rawRequest = "POST / HTTP/1.1\r\nContent-Length: " + expectedBody.length
                + "\r\n\r\n你好\r\n\r\n";
        ByteArrayInputStream input = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 2));
            }
        };
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        HttpRequest request = decoder.decode(input);

        assertNotNull(request);
        assertEquals("POST", request.getHttpRequestHeader().requestLine().method());
        assertArrayEquals(expectedBody, request.getHttpBody());
        assertNull(decoder.decode(input));
    }

    @Test
    void shouldDecodeConsecutiveRequestsWithoutMixingHeadersOrBodies() throws IOException {
        ByteArrayInputStream input = stream(
                "POST /empty HTTP/1.1\r\nContent-Length: 0\r\n\r\n"
                        + "POST /body HTTP/1.1\r\nContent-Length: 5\r\n\r\nhello"
                        + "GET /next HTTP/1.1\r\nHost: localhost\r\n\r\n"
        );
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        HttpRequest empty = decoder.decode(input);
        HttpRequest withBody = decoder.decode(input);
        HttpRequest next = decoder.decode(input);

        assertNotNull(empty);
        assertNotNull(withBody);
        assertNotNull(next);
        assertEquals("/empty", empty.getHttpRequestHeader().requestLine().target());
        assertArrayEquals(new byte[0], empty.getHttpBody());
        assertEquals("/body", withBody.getHttpRequestHeader().requestLine().target());
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), withBody.getHttpBody());
        assertEquals("/next", next.getHttpRequestHeader().requestLine().target());
        assertTrue(next.getHttpRequestHeader().headerFields().get("Content-Length").isEmpty());
        assertArrayEquals(new byte[0], next.getHttpBody());
        assertNull(decoder.decode(input));
    }

    @Test
    void shouldRejectNonNumericContentLength() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(stream(
                "POST / HTTP/1.1\r\nContent-Length: abc\r\n\r\n"
        )));
    }

    @Test
    void shouldRejectDuplicateContentLengthFields() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(stream(
                "POST / HTTP/1.1\r\nContent-Length: 5\r\nContent-Length: 5\r\n\r\nhello"
        )));
    }

    @Test
    void shouldRejectBodyLengthAboveOneMiB() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(stream(
                "POST / HTTP/1.1\r\nContent-Length: 1048577\r\n\r\n"
        )));
    }

    @Test
    void shouldRejectContentLengthTogetherWithTransferEncoding() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(IllegalArgumentException.class, () -> decoder.decode(stream(
                "POST / HTTP/1.1\r\nContent-Length: 0\r\nTransfer-Encoding: chunked\r\n\r\n"
        )));
    }

    @Test
    void shouldRejectUnsupportedTransferEncoding() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(UnsupportedOperationException.class, () -> decoder.decode(stream(
                "POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n0\r\n\r\n"
        )));
    }

    @Test
    void shouldThrowEOFExceptionWhenBodyIsShorterThanContentLength() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(EOFException.class, () -> decoder.decode(stream(
                "POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\nabc"
        )));
    }

    @Test
    void shouldRejectNegativeContentLength() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(IllegalArgumentException.class, () ->
                decoder.decode(stream(
                        "POST / HTTP/1.1\r\n" +
                                "Content-Length: -1\r\n\r\n"
                ))
        );
    }

    @Test
    void shouldRejectContentLengthOverflow() {
        HttpRequestDecoder decoder = new HttpRequestDecoder();

        assertThrows(IllegalArgumentException.class, () ->
                decoder.decode(stream(
                        "POST / HTTP/1.1\r\n" +
                                "Content-Length: 999999999999999999999\r\n\r\n"
                ))
        );
    }

    private ByteArrayInputStream stream(String request) {
        return new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
    }
}
