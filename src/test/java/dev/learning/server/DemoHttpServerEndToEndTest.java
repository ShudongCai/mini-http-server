package dev.learning.server;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DemoHttpServerEndToEndTest {
    @Test
    void shouldReturnOnly200OKForValidGet() throws Exception {
        String response = sendRequest(
                "GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n"
        );

        assertSingleResponse(response, "HTTP/1.1 200 OK");
    }

    @Test
    void shouldReturnOnly400BadRequestForInvalidContentLength() throws Exception {
        String response = sendRequest(
                "POST /hello HTTP/1.1\r\nHost: localhost\r\nContent-Length: abc\r\n\r\n"
        );

        assertSingleResponse(response, "HTTP/1.1 400 Bad Request");
    }

    @Test
    void shouldReturnOnly501NotImplementedForChunkedTransferEncoding() throws Exception {
        String response = sendRequest(
                "POST /hello HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n"
                        + "0\r\n\r\n"
        );

        assertSingleResponse(response, "HTTP/1.1 501 Not Implemented");
    }

    private String sendRequest(String request) throws Exception {
        DemoHttpServer server = new DemoHttpServer(0);
        server.listen();
        Thread serverThread = null;

        try {
            // The public API does not expose the OS-assigned port; keep this access in test setup.
            Field socketField = DemoHttpServer.class.getDeclaredField("serverSocket");
            socketField.setAccessible(true);
            ServerSocket listeningSocket = (ServerSocket) socketField.get(server);

            FutureTask<Void> handling = new FutureTask<>(server::acceptAndRespondOnce, null);
            serverThread = Thread.ofPlatform().daemon(true).start(handling);

            try (Socket client = new Socket()) {
                client.connect(new InetSocketAddress("127.0.0.1", listeningSocket.getLocalPort()), 5000);
                client.setSoTimeout(5000);
                client.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
                client.getOutputStream().flush();

                // Read until the server closes the connection, so any extra response is included.
                String response = new String(client.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                handling.get(5, TimeUnit.SECONDS);
                return response;
            }
        } finally {
            server.close();
            if (serverThread != null) {
                serverThread.join(5000);
                assertFalse(serverThread.isAlive(), "Server thread did not stop");
            }
        }
    }

    private void assertSingleResponse(String response, String expectedStatusLine) {
        String[] sections = response.split("\r\n\r\n", 2);
        assertEquals(2, sections.length, "Response must contain a complete header section");
        assertEquals(expectedStatusLine, sections[0].split("\r\n", 2)[0]);
        assertEquals(response.indexOf("HTTP/1.1 "), response.lastIndexOf("HTTP/1.1 "),
                "Each request must receive exactly one response");

        List<String> contentLengths = sections[0].lines()
                .filter(line -> line.startsWith("Content-Length: "))
                .toList();
        assertEquals(1, contentLengths.size(), "Response must contain one Content-Length field");
        int contentLength = Integer.parseInt(contentLengths.getFirst().substring("Content-Length: ".length()));
        assertEquals(contentLength, sections[1].getBytes(StandardCharsets.UTF_8).length,
                "No bytes may follow the declared response body");
    }
}
