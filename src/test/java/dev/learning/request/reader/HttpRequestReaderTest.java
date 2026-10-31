package dev.learning.request.reader;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class HttpRequestReaderTest {
    @Test
    void shouldReadCompleteHeader() throws IOException {
        //Arrange
        String request = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n";

        byte[] expected = request.getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream input = new ByteArrayInputStream(expected);
        HttpRequestReader reader = new HttpRequestReader();

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
        HttpRequestReader reader = new HttpRequestReader();

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
        HttpRequestReader reader = new HttpRequestReader();

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
        HttpRequestReader reader = new HttpRequestReader();

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
        HttpRequestReader reader = new HttpRequestReader();

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
        HttpRequestReader reader = new HttpRequestReader();

        //Act
        byte[] actual = reader.readHead(stream);

        //Arrange
        assertArrayEquals(request.getBytes(StandardCharsets.UTF_8), actual);
        assertEquals(0, stream.available());
    }

    @Test
    void shouldReadBodyFromPendingAndPreserveNextRequest() throws IOException {
        String head = "POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n";
        String nextHead = "GET /next HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(
                (head + "hello" + nextHead).getBytes(StandardCharsets.US_ASCII)
        );
        HttpRequestReader reader = new HttpRequestReader();

        assertArrayEquals(head.getBytes(StandardCharsets.US_ASCII), reader.readHead(stream));
        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), reader.readExactly(stream, 5));
        assertArrayEquals(nextHead.getBytes(StandardCharsets.US_ASCII), reader.readHead(stream));
        assertNull(reader.readHead(stream));
    }

    @Test
    void shouldCombinePendingBodyWithFragmentedStream() throws IOException {
        String head = "POST / HTTP/1.1\r\nContent-Length: 10\r\n\r\n";
        String body = "abcdefghij";
        String nextHead = "GET /next HTTP/1.1\r\nHost: localhost\r\n\r\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(
                (head + body + nextHead).getBytes(StandardCharsets.US_ASCII)
        ) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                // The first read includes two body bytes; later reads return at most two bytes.
                int chunkSize = pos == 0 ? head.length() + 2 : 2;
                return super.read(b, off, Math.min(len, chunkSize));
            }
        };
        HttpRequestReader reader = new HttpRequestReader();

        assertArrayEquals(head.getBytes(StandardCharsets.US_ASCII), reader.readHead(stream));
        assertArrayEquals(body.getBytes(StandardCharsets.US_ASCII), reader.readExactly(stream, 10));
        assertArrayEquals(nextHead.getBytes(StandardCharsets.US_ASCII), reader.readHead(stream));
    }

    @Test
    void shouldRejectIncompleteBodyAfterReadingHead() throws IOException {
        String head = "POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(
                (head + "abc").getBytes(StandardCharsets.US_ASCII)
        );
        HttpRequestReader reader = new HttpRequestReader();
        reader.readHead(stream);

        assertThrows(EOFException.class, () -> reader.readExactly(stream, 5));
    }

    @Test
    void shouldReadExactBinaryBytesFromFragmentedStreamWithoutOverreading() throws IOException {
        byte[] body = {0, (byte) 0xFF, '\r', '\n', '\r', '\n'};
        byte[] input = {0, (byte) 0xFF, '\r', '\n', '\r', '\n', 42};
        ByteArrayInputStream stream = new ByteArrayInputStream(input) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 1));
            }
        };
        HttpRequestReader reader = new HttpRequestReader();

        assertArrayEquals(body, reader.readExactly(stream, body.length));
        assertEquals(42, stream.read());
        assertEquals(-1, stream.read());
    }

    @Test
    void shouldReturnEmptyArrayWhenByteCountIsZero() throws IOException {
        HttpRequestReader reader = new HttpRequestReader();

        byte[] actual = reader.readExactly(
                new ByteArrayInputStream(new byte[]{1, 2, 3}),
                0
        );

        assertArrayEquals(new byte[0], actual);
    }

    @Test
    void shouldRejectNegativeByteCount() {
        HttpRequestReader reader = new HttpRequestReader();

        assertThrows(
                IllegalArgumentException.class,
                () -> reader.readExactly(
                        new ByteArrayInputStream(new byte[0]),
                        -1
                )
        );
    }
}
