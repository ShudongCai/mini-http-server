
package dev.learning.request.parser;

import dev.learning.request.model.HttpHeaderFields;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HttpHeaderFieldsParserTest {

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void shouldProvideHttpHeaderWhenValid() {
        // Arrange
        String rawHeader =
                "Host: localhost\r\n" +
                        "Accept: application/json\r\n" +
                        "\r\n";

        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act
        HttpHeaderFields headers = parser.parse(bytes(rawHeader));

        // Assert
        assertEquals(List.of("localhost"), headers.get("Host"));
        assertEquals(List.of("application/json"), headers.get("Accept"));
    }

    @Test
    void shouldPreserveMultipleValues() {
        // Arrange
        String rawHeader =
                "Accept: application/json\r\n" +
                        "accept: text/html\r\n" +
                        "\r\n";

        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act
        HttpHeaderFields headers = parser.parse(bytes(rawHeader));

        // Assert
        assertEquals(
                List.of("application/json", "text/html"),
                headers.get("ACCEPT")
        );
    }

    @Test
    void shouldAllowNoSpaceAfterColon() {
        // Arrange
        String rawHeader =
                "Content-Type:application/json\r\n" +
                        "Host:localhost:8080\r\n" +
                        "\r\n";

        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act
        HttpHeaderFields headers = parser.parse(bytes(rawHeader));

        // Assert
        assertEquals(
                List.of("application/json"),
                headers.get("Content-Type")
        );

        assertEquals(
                List.of("localhost:8080"),
                headers.get("Host")
        );
    }

    @Test
    void shouldTrimOptionalWhitespace() {
        // Arrange
        String rawHeader =
                "X-Test: \t Hello\tWorld \t\r\n" +
                        "\r\n";

        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act
        HttpHeaderFields headers = parser.parse(bytes(rawHeader));

        // Assert
        assertEquals(
                List.of("Hello\tWorld"),
                headers.get("X-Test")
        );
    }

    @Test
    void shouldRejectInvalidCRLF() {
        // Arrange
        String rawHeader = "Host: localhost\rX\r\n\r\n";
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(bytes(rawHeader))
        );
    }

    @Test
    void shouldRejectBareLF() {
        // Arrange
        String rawHeader = "Host: localhost\n\r\n";
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(bytes(rawHeader))
        );
    }

    @Test
    void shouldRejectMissingEmptyLine() {
        // Arrange
        String rawHeader = "Host: localhost\r\n";
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(bytes(rawHeader))
        );
    }

    @Test
    void shouldRejectMissingColon() {
        // Arrange
        String rawHeader = "Host localhost\r\n\r\n";
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(bytes(rawHeader))
        );
    }

    @Test
    void shouldRejectWhitespaceBeforeColon() {
        // Arrange
        String rawHeader = "Host : localhost\r\n\r\n";
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(bytes(rawHeader))
        );
    }

    @Test
    void shouldNotShareHeadersBetweenRequests() {
        // Arrange
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act
        HttpHeaderFields first = parser.parse(
                bytes("Host: example.com\r\n\r\n")
        );

        HttpHeaderFields second = parser.parse(
                bytes("Accept: text/html\r\n\r\n")
        );

        // Assert
        assertNotSame(first, second);

        assertEquals(List.of("example.com"), first.get("Host"));
        assertTrue(first.get("Accept").isEmpty());

        assertEquals(List.of("text/html"), second.get("Accept"));
        assertTrue(second.get("Host").isEmpty());
    }

    @Test
    void shouldAcceptEmptyHeaderSection() {
        // Arrange
        HttpHeaderFieldsParser parser = new HttpHeaderFieldsParser();

        // Act
        HttpHeaderFields headers = parser.parse(bytes("\r\n"));

        // Assert
        assertTrue(headers.get("Host").isEmpty());
    }
}
