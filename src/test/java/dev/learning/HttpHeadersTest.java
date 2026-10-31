package dev.learning;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpHeadersTest {
    @Test
    void shouldIgnoreHeaderNameCase() {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Accept", "application/json");
        headers.add("accept", "text/html");

        assertEquals(
                List.of("application/json", "text/html"),
                headers.get("ACCEPT")
        );
    }

    @Test
    void shouldRejectInvalidHeaderName() {
        HttpHeaders headers = new HttpHeaders();

        assertThrows(
                IllegalArgumentException.class,
                () -> headers.add("Content Type", "application/json")
        );
    }

    @Test
    void shouldNotExposeMutableValues() {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Accept", "application/json");

        List<String> values = headers.get("Accept");

        assertThrows(
                UnsupportedOperationException.class,
                () -> values.add("text/html")
        );
    }

    @Test
    void shouldAcceptHyphenatedHeaderName() {
        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Type", "application/json");

        assertEquals(
                List.of("application/json"),
                headers.get("content-type")
        );
    }

    @Test
    void shouldReturnEmptyListForMissingHeader() {
        HttpHeaders headers = new HttpHeaders();

        assertEquals(List.of(), headers.get("missing"));
    }

    @Test
    void shouldRejectHeaderValueContainingCRLF() {
        HttpHeaders headers = new HttpHeaders();

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
        HttpHeaders headers = new HttpHeaders();

        headers.add("X-Test", "Hello\tWorld");

        assertEquals(
                List.of("Hello\tWorld"),
                headers.get("X-Test")
        );
    }
}
