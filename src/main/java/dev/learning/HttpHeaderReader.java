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

    public void readHeaders(InputStream stream) {
        try {
            while (true) {
                int headerEnd = findHeaderEnd();

                if (headerEnd >= 0) {
                    processHeader(headerEnd);
                    continue;
                }

                int bytesRead = stream.read(readBuffer);

                if (bytesRead == -1) {
                    if (pendingSize > 0) {
                        throw new EOFException("Incomplete HTTP headers");
                    }
                    return;
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    private void processHeader(int headerEnd) {
        int endExclusive = headerEnd + 1;

        byte[] header = Arrays.copyOfRange(
                pending, 0, endExclusive
        );

        // Temporary debugging; later pass to HTTP parser
        System.out.println(
                new String(header, StandardCharsets.UTF_8)
        );

        int remaining = pendingSize - endExclusive;

        System.arraycopy(
                pending, endExclusive,
                pending, 0,
                remaining
        );

        pendingSize = remaining;
    }
}

