package dev.learning.request.reader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class HttpRequestReader {
    private static final int MAX_HEADER_SIZE = 16 * 1024;
    private static final int READ_BUFFER_SIZE = 4 * 1024;

    private final byte[] readBuffer = new byte[READ_BUFFER_SIZE];
    private final byte[] pending = new byte[MAX_HEADER_SIZE];

    private int pendingSize = 0;

    public byte[] readHead(InputStream stream) throws IOException{
        while (true) {
            int headerEnd = findHeadEnd();

            if (headerEnd >= 0) {
                return consume(headerEnd + 1);
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

    private int findHeadEnd() {
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

    private byte[] consume(int byteCount) {
        if (byteCount < 0 || byteCount > pendingSize) {
            throw new IllegalArgumentException("Invalid byte count");
        }

        byte[] bytesContent = Arrays.copyOfRange(
                pending, 0, byteCount
        );

        int remaining = pendingSize - byteCount;

        System.arraycopy(
                pending, byteCount,
                pending, 0,
                remaining
        );

        pendingSize = remaining;

        return bytesContent;
    }

    public byte[] readExactly(InputStream stream, int byteCount) throws IOException {
        if (pendingSize >= byteCount) {
            return consume(byteCount);
        }

        byte[] contentInStream = new byte[byteCount - pendingSize];
        int offset = 0;

        while (offset < contentInStream.length) {
            int bytesRead =
                    stream.read(contentInStream, offset, contentInStream.length - offset);

            if (bytesRead == -1) {
                throw new EOFException("Incomplete HTTP request body");
            }

            offset += bytesRead;
        }

        byte[] restContentInPending = consume(pendingSize);

        byte[] result = new byte[byteCount];

        System.arraycopy(
                restContentInPending,
                0,
                result,
                0,
                restContentInPending.length
        );

        System.arraycopy(
                contentInStream,
                0,
                result,
                restContentInPending.length,
                contentInStream.length
        );

        return result;
    }
}

