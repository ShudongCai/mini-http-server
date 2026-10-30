package dev.learning;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HttpHeaderReader {
    private static final int MAX_HEADER_SIZE = 16 * 1024;
    private static final int READ_BUFFER_SIZE = 4 * 1024;

    private final byte[] readBuffer;
    private final byte[] pending;
    private int offset;

    public HttpHeaderReader() {
        this.readBuffer = new byte[READ_BUFFER_SIZE];
        this.pending = new byte[MAX_HEADER_SIZE];
        this.offset = 0;
    }

    public void readHeaders(InputStream stream) {
        try {
            while (true) {
                int result = searchForRequest();

                if (result == -1) {
                    int bytesRead = stream.read(readBuffer);

                    if (bytesRead == -1) {
                        break;
                    }

                    if (bytesRead > 0 && offset + bytesRead <= MAX_HEADER_SIZE) {
                        System.arraycopy(readBuffer, 0, pending, offset, bytesRead);
                        offset += bytesRead;
                    } else {
                        throw new IllegalArgumentException("Header too long");
                    }
                } else {
                    //need http header reader to read the header
                    byte[] header =
                            Arrays.copyOfRange(
                                    pending,
                                    0,
                                    result + 1
                            );
                    System.out.println(new String(header, StandardCharsets.UTF_8));
                    System.arraycopy(pending, result + 1, pending, 0, offset - result - 1);
                    offset = offset - result - 1;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int searchForRequest() {
        int position = 0;
        byte[] temp = pending;

        while (position < offset) {
            if (temp[position] != '\r') {
                position++;
                continue;
            } else if (position + 3 < offset){
                if (temp[position + 1] == '\n'
                    && temp[position + 2] == '\r'
                    && temp[position + 3] == '\n') {
                    return position + 3;
                }
            }
            position++;
        }
        return -1;
    }
}
