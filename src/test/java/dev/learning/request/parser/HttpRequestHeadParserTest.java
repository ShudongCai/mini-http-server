package dev.learning.request.parser;

import dev.learning.request.model.HttpRequestHeader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRequestHeadParserTest {
    @Test
    void shouldRejectIfInputIsNull() {
        //Arrange
        HttpRequestHeadParser parser = new HttpRequestHeadParser();

        //Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(null)
        );
    }

    @Test
    void shouldRejectEmptyInput() {
        HttpRequestHeadParser parser = new HttpRequestHeadParser();

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new byte[0])
        );
    }

    @Test
    void shouldRejectMissingCarriageReturn() {
        //Arrange
        String request = "GET / HTTP/1.1\nHost: localhost\r\n\r\n";
        HttpRequestHeadParser parser = new HttpRequestHeadParser();

        //Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(request.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void shouldRejectMissingLineFeed() {
        //Arrange
        String request = "GET / HTTP/1.1\rHost: localhost\r\n\r\n";
        HttpRequestHeadParser parser = new HttpRequestHeadParser();

        //Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(request.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void shouldRejectMissingHeaderTerminator() {
        String request =
                "GET / HTTP/1.1\r\n" +
                        "Host: localhost\r\n";

        HttpRequestHeadParser parser = new HttpRequestHeadParser();

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(
                        request.getBytes(StandardCharsets.US_ASCII)
                )
        );
    }

    @Test
    void shouldReturnIfInputIsValid() {
        //Arrange
        String request = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";
        HttpRequestHeadParser parser = new HttpRequestHeadParser();

        //Act
        HttpRequestHeader actual = parser.parse(request.getBytes(StandardCharsets.UTF_8));

        //Assert
        assertNotNull(actual.requestLine());
        assertNotNull(actual.headerFields());

        assertEquals(
                "GET",
                actual.requestLine().method()
        );

        assertEquals(
                "/",
                actual.requestLine().target()
        );

        assertEquals(
                "HTTP/1.1",
                actual.requestLine().version()
        );

        assertEquals(
                List.of("localhost"),
                actual.headerFields().get("Host")
        );
    }
}
