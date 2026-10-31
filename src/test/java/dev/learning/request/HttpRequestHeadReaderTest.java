package dev.learning.request;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRequestHeadReaderTest {
    @Test
    void shouldReadCompleteHeader() throws IOException {
        //Arrange
        String request = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";

        byte[] expected = request.getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream input = new ByteArrayInputStream(expected);
        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        //Act
        byte[] actual = reader.readHead(input);

        //Assert
        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldPreserveNextRequest() throws IOException {
        //Arrange
        String first = "GET /a HTTP/1.1\r\nHost: localhost\r\n\r\n";
        String second = "GET /b HTTP/1.1\r\nHost: localhost\r\n\r\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(
                (first + second).getBytes(StandardCharsets.UTF_8)
        );
        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        //Act
        byte[] actualFirst = reader.readHead(stream);
        byte[] actualSecond = reader.readHead(stream);

        //Assert
        assertArrayEquals(first.getBytes(StandardCharsets.UTF_8), actualFirst);
        assertArrayEquals(second.getBytes(StandardCharsets.UTF_8), actualSecond);
    }

    @Test
    void shouldRejectIncompleteHeader() {
        //Arrange
        String first = "GET /a HTTP/1.1\r\nHost: localhost\r\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(
                first.getBytes(StandardCharsets.UTF_8)
        );
        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        //Act + Assert
        assertThrows(EOFException.class, () -> reader.readHead(stream));
    }

    @Test
    void shouldHandleFragmentedInput() throws IOException {
        //Arrange
        String first = "GET /a HTTP/1.1\r\nHost: localhost\r\n\r\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(first.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 3));
            }
        };
        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        //Act
        byte[] actual = reader.readHead(stream);

        //Assert
        assertArrayEquals(first.getBytes(StandardCharsets.UTF_8), actual);
    }

    @Test
    void shouldRejectOversizedHeader() {
        //Arrange
        String prefix = "GET / HTTP/1.1\r\nX-Pad: ";
        String suffix = "\r\n\r\n";

        int targetSize = 16 * 1024 + 1;

        String request = prefix
                + "a".repeat(targetSize - prefix.length() - suffix.length())
                + suffix;

        ByteArrayInputStream stream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        assertEquals(16385, request.getBytes(StandardCharsets.UTF_8).length);

        //Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> reader.readHead(stream)
        );
    }

    @Test
    void shouldHandleHeaderAtBufferBoundary() throws IOException {
        //Arrange
        String prefix = "GET / HTTP/1.1\r\nX-Pad: ";

        String request = prefix
                + "a".repeat(4094 - prefix.length())
                + "\r\n\r\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));
        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        //Act
        byte[] actual = reader.readHead(stream);

        //Arrange
        assertArrayEquals(request.getBytes(StandardCharsets.UTF_8), actual);
        assertEquals(0, stream.available());
    }
}
