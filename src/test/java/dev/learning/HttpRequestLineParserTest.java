package dev.learning;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpRequestLineParserTest {
    @Test
    void shouldParseOnlyFirstRequestLine() {
        // Arrange
        String rawHeader =
                "GET /products HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Accept: application/json\r\n" +
                        "\r\n";

        HttpRequestLineParser parser = new HttpRequestLineParser();

        // Act
        HttpRequestLine actual = parser.parseHttpRequestLine(
                rawHeader.getBytes(StandardCharsets.US_ASCII)
        );

        // Assert
        assertEquals("GET", actual.method());
        assertEquals("/products", actual.target());
        assertEquals("HTTP/1.1", actual.version());
    }

    @Test
    void shouldRejectExtraSpaces() {
        //Arrange
        String rawHeader = "GET /a HTTP/1.1 EXTRA\r\n";

        HttpRequestLineParser parser = new HttpRequestLineParser();

        //Act + Assert
         assertThrows(
                 IllegalArgumentException.class,
                 () -> parser.parseHttpRequestLine(
                rawHeader.getBytes(StandardCharsets.UTF_8))
         );
    }

    @Test
    void shouldRejectEmptyMethod() {
        //Arrange
        String rawHeader = " /a HTTP/1.1\r\n";

        HttpRequestLineParser parser = new HttpRequestLineParser();

        //Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseHttpRequestLine(
                        rawHeader.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void shouldRejectEmptyTarget() {
        //Arrange
        String rawHeader = "GET  HTTP/1.1\r\n";

        HttpRequestLineParser parser = new HttpRequestLineParser();

        //Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseHttpRequestLine(
                        rawHeader.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void shouldRejectEmptyVersion() {
        //Arrange
        String rawHeader = "GET /a \r\n";

        HttpRequestLineParser parser = new HttpRequestLineParser();

        //Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseHttpRequestLine(
                        rawHeader.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void shouldRejectBareLineFeed() {
        String rawHeader = "GET /products HTTP/1.1\nHost: localhost\r\n";

        HttpRequestLineParser parser = new HttpRequestLineParser();

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseHttpRequestLine(
                        rawHeader.getBytes(StandardCharsets.US_ASCII)
                )
        );
    }
}
