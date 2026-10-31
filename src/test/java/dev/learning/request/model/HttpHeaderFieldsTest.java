package dev.learning.request.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpHeaderFieldsTest {
    @Test
    void shouldIgnoreHeaderNameCase() {
        HttpHeaderFields headers = new HttpHeaderFields();

        headers.add("Accept", "application/json");
        headers.add("accept", "text/html");

        assertEquals(
                List.of("application/json", "text/html"),
                headers.get("ACCEPT")
        );
    }

    @Test
    void shouldRejectInvalidHeaderName() {
        HttpHeaderFields headers = new HttpHeaderFields();

        assertThrows(
                IllegalArgumentException.class,
                () -> headers.add("Content Type", "application/json")
        );
    }

    @Test
    void shouldNotExposeMutableValues() {
        HttpHeaderFields headers = new HttpHeaderFields();

        headers.add("Accept", "application/json");

        List<String> values = headers.get("Accept");

        assertThrows(
                UnsupportedOperationException.class,
                () -> values.add("text/html")
        );
    }

    @Test
    void shouldAcceptHyphenatedHeaderName() {
        HttpHeaderFields headers = new HttpHeaderFields();

        headers.add("Content-Type", "application/json");

        assertEquals(
                List.of("application/json"),
                headers.get("content-type")
        );
    }

    @Test
    void shouldReturnEmptyListForMissingHeader() {
        HttpHeaderFields headers = new HttpHeaderFields();

        assertEquals(List.of(), headers.get("missing"));
    }

    @Test
    void shouldRejectHeaderValueContainingCRLF() {
        HttpHeaderFields headers = new HttpHeaderFields();

        assertThrows(
                IllegalArgumentException.class,
                () -> headers.add(
                        "X-Test",
                        "hello\r\nInjected: value"
                )
        );
    }

    @Test
    void shouldAcceptTabInsideHeaderValue() {
        HttpHeaderFields headers = new HttpHeaderFields();

        headers.add("X-Test", "Hello\tWorld");

        assertEquals(
                List.of("Hello\tWorld"),
                headers.get("X-Test")
        );
    }
}
