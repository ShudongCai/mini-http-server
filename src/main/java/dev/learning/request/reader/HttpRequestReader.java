package dev.learning.request.reader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Reads request heads and fixed-length byte sequences without parsing HTTP fields.
 *
 * <p>Use one instance sequentially with the same connection's input stream. This
 * class is not thread-safe and does not close the stream. Its pending buffer holds
 * bytes already read from the stream but not yet consumed, including body bytes
 * or bytes belonging to a later request. Failed reads do not roll back stream state.
 */
public class HttpRequestReader {
    private static final int MAX_HEADER_SIZE = 16 * 1024;
    private static final int READ_BUFFER_SIZE = 4 * 1024;

    private final byte[] readBuffer = new byte[READ_BUFFER_SIZE];
    private final byte[] pending = new byte[MAX_HEADER_SIZE];

    private int pendingSize = 0;

    /**
     * Reads through the first CRLFCRLF terminator, including the terminator.
     *
     * <p>The returned bytes contain the request line and header fields; their syntax
     * is not validated here. Any bytes read beyond the terminator remain pending
     * for subsequent calls to this reader.
     *
     * @param stream the input stream for this connection
     * @return the head bytes, or null if EOF is reached with no pending bytes
     * @throws EOFException if EOF is reached with an incomplete head
     * @throws IllegalArgumentException if a read would exceed the 16 KiB pending capacity
     * @throws IOException if another I/O error occurs
     */
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

    /**
     * Consumes pending bytes first, then reads only the additional bytes needed.
     *
     * @param stream the input stream for this connection
     * @param byteCount the non-negative number of bytes to consume
     * @return exactly {@code byteCount} bytes, or an empty array for zero
     * @throws IllegalArgumentException if {@code byteCount} is negative
     * @throws EOFException if fewer than {@code byteCount} bytes are available before EOF
     * @throws IOException if another I/O error occurs
     */
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

