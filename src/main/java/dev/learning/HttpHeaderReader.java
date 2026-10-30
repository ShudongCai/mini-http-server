package dev.learning;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HttpHeaderReader {
    private static final int MAX_HEADER_SIZE = 16 * 1024;
    private static final int READ_BUFFER_SIZE = 4 * 1024;

    private final byte[] readBuffer = new byte[READ_BUFFER_SIZE];
    private final byte[] pending = new byte[MAX_HEADER_SIZE];

    private int pendingSize = 0;

    public byte[] readHeaders(InputStream stream) throws IOException{
        while (true) {
            int headerEnd = findHeaderEnd();

            if (headerEnd >= 0) {
                return processHeader(headerEnd);
            }

            int bytesRead = stream.read(readBuffer);

            if (bytesRead == -1) {
                if (pendingSize > 0) {
                    throw new EOFException("Incomplete HTTP headers");
                }
                return null;
            }

            if (bytesRead > pending.length - pendingSize) {
                throw new IllegalArgumentException("Buffer capacity exceeded");
            }

            System.arraycopy(
                    readBuffer, 0,
                    pending, pendingSize,
                    bytesRead
            );

            pendingSize += bytesRead;
        }
    }

    private int findHeaderEnd() {
        for (int i = 0; i + 3 < pendingSize; i++) {
            if (pending[i] == '\r'
                    && pending[i + 1] == '\n'
                    && pending[i + 2] == '\r'
                    && pending[i + 3] == '\n') {
                return i + 3;
            }
        }

        return -1;
    }

    private byte[] processHeader(int headerEnd) {
        int endExclusive = headerEnd + 1;

        byte[] header = Arrays.copyOfRange(
                pending, 0, endExclusive
        );

        int remaining = pendingSize - endExclusive;

        System.arraycopy(
                pending, endExclusive,
                pending, 0,
                remaining
        );

        pendingSize = remaining;

        return header;
    }
}

