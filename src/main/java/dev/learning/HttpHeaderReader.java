package dev.learning;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class HttpHeaderReader {
    private final byte[] readBuffer;
    private final ByteArrayOutputStream pending;
    private int offset;

    public HttpHeaderReader() {
        this.readBuffer = new byte[4096];
        this.pending = new ByteArrayOutputStream();
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

                    if (bytesRead > 0) {
                        pending.write(readBuffer, 0, bytesRead);
                    }
                } else {
                    byte[] data = pending.toByteArray();
                    //need http header reader to read the header
                    byte[] header =
                            Arrays.copyOfRange(
                                    data,
                                    offset,
                                    result + 1
                            );
                    offset = result + 1;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int searchForRequest() {
        int position = offset;
        byte[] temp = pending.toByteArray();

        while (position < temp.length) {
            if (temp[position] != '\r') {
                position++;
                continue;
            } else if (position + 3 < temp.length){
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
