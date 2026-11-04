package dev.learning.server;

import dev.learning.response.HttpResponse;
import dev.learning.router.HttpRouter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DemoHttpServerKeepAliveTest {
    private static final String HELLO_REQUEST =
            "GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n";
    private static final String ECHO_REQUEST =
            "POST /echo HTTP/1.1\r\nHost: localhost\r\nContent-Length: 5\r\n\r\nHello";

    private DemoHttpServer server;
    private Socket client;
    private int port;
    private ExecutorService connectionTasks;
    private FutureTask<Void> acceptance;
    private Thread acceptThread;
    private final CountDownLatch sharedHandlersReady = new CountDownLatch(2);
    private final HttpResponse sharedResponse = HttpResponse.builder()
            .setStatusCode(202)
            .setReasonPhrase("Accepted")
            .setContent("Shared response")
            // A distinct value exposes mutation regardless of which request finishes last.
            .setConnection("handler-default")
            .build();

    @BeforeEach
    void startServerAndConnect() throws Exception {
        HttpRouter router = new HttpRouter();
        router.register("GET", "/hello", request ->
                HttpResponse.builder().setContent("Hello World").build());
        router.register("POST", "/echo", request -> HttpResponse.builder()
                .setContent("You sent: " + new String(request.getHttpBody(), StandardCharsets.UTF_8))
                .build());
        router.register("GET", "/shared", request -> {
            sharedHandlersReady.countDown();
            try {
                if (!sharedHandlersReady.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The two handlers did not run concurrently");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while synchronizing handlers", e);
            }
            return sharedResponse;
        });

        DemoHttpServer startedServer = new DemoHttpServer(0, router);
        startedServer.listen();
        server = startedServer;

        // The current public API does not expose the OS-assigned port.
        Field socketField = DemoHttpServer.class.getDeclaredField("serverSocket");
        socketField.setAccessible(true);
        ServerSocket listeningSocket = (ServerSocket) socketField.get(server);
        port = listeningSocket.getLocalPort();
        Field executorField = DemoHttpServer.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        connectionTasks = (ExecutorService) executorField.get(server);

        acceptance = new FutureTask<>(server::acceptAndRespondOnce, null);
        acceptThread = Thread.ofPlatform().daemon(true).start(acceptance);
        client = new Socket();
        client.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        client.setSoTimeout(5000);
    }

    @AfterEach
    void closeConnectionAndServer() throws Exception {
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            try {
                if (server != null) {
                    server.close();
                }
            } finally {
                if (acceptThread != null) {
                    acceptThread.join(5000);
                    assertFalse(acceptThread.isAlive(), "Accept thread did not stop");
                }
                if (connectionTasks != null) {
                    assertTrue(connectionTasks.awaitTermination(5, TimeUnit.SECONDS),
                            "Connection tasks did not stop after shutdown");
                }
            }
        }
        if (acceptance != null) {
            acceptance.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldReturn200AndKeepConnectionAliveForFirstHelloRequest() throws Exception {
        Response response = exchange(HELLO_REQUEST);

        assertEquals("HTTP/1.1 200 OK", response.statusLine());
        assertEquals("keep-alive", response.connection());
        assertEquals("Hello World", response.body());
    }

    @Test
    void shouldEchoBodyOnSameConnectionAfterHelloRequest() throws Exception {
        Response first = exchange(HELLO_REQUEST);
        assertEquals("HTTP/1.1 200 OK", first.statusLine());

        Response second = exchange(ECHO_REQUEST);

        assertEquals("HTTP/1.1 200 OK", second.statusLine());
        assertEquals("You sent: Hello", second.body());
        assertEquals("keep-alive", second.connection());
    }

    @Test
    void shouldCloseConnectionAfterThirdRequestWithCloseOption() throws Exception {
        Response first = exchange(HELLO_REQUEST);
        assertEquals("HTTP/1.1 200 OK", first.statusLine());
        assertEquals("keep-alive", first.connection());

        Response second = exchange(ECHO_REQUEST);
        assertEquals("HTTP/1.1 200 OK", second.statusLine());
        assertEquals("You sent: Hello", second.body());
        assertEquals("keep-alive", second.connection());

        Response third = exchange(
                "GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
        );

        assertEquals("HTTP/1.1 200 OK", third.statusLine());
        assertEquals("Hello World", third.body());
        assertEquals("close", third.connection());
        assertEquals(-1, client.getInputStream().read(),
                "Server must close the connection after the complete third response");
    }

    @Test
    void shouldNotModifySharedResponseAcrossConnections() throws Exception {
        String originalResponse = sharedResponse.toHttpString();
        acceptance.get(5, TimeUnit.SECONDS);
        try (Socket closingClient = new Socket()) {
            closingClient.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            closingClient.setSoTimeout(5000);
            // The fixture accepts the first connection; accept and submit the second one here.
            server.acceptAndRespondOnce();

            sendRequest(client, "GET /shared HTTP/1.1\r\nHost: localhost\r\n\r\n");
            sendRequest(closingClient,
                    "GET /shared HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");

            Response keepAlive = readResponse(client);
            Response closing = readResponse(closingClient);

            assertEquals(0L, sharedHandlersReady.getCount());
            assertEquals("HTTP/1.1 202 Accepted", keepAlive.statusLine());
            assertEquals("HTTP/1.1 202 Accepted", closing.statusLine());
            assertEquals("Shared response", keepAlive.body());
            assertEquals("Shared response", closing.body());
            assertEquals("keep-alive", keepAlive.connection());
            assertEquals("close", closing.connection());
            assertEquals(-1, closingClient.getInputStream().read(),
                    "Only the connection requesting close must reach EOF");
            assertEquals("handler-default", sharedResponse.getConnection());
            assertEquals(originalResponse, sharedResponse.toHttpString());

            // Prove the other connection remains usable, rather than trusting its header alone.
            Response followUp = exchange(
                    "GET /shared HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");
            assertEquals("HTTP/1.1 202 Accepted", followUp.statusLine());
            assertEquals("Shared response", followUp.body());
            assertEquals("close", followUp.connection());
            assertEquals(-1, client.getInputStream().read());
            assertEquals(originalResponse, sharedResponse.toHttpString());
        }
    }

    private Response exchange(String request) throws Exception {
        sendRequest(client, request);
        return readResponse(client);
    }

    private void sendRequest(Socket connection, String request) throws Exception {
        connection.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
        connection.getOutputStream().flush();
    }

    private Response readResponse(Socket socket) throws Exception {
        // Frame each response by Content-Length: keep-alive does not end at EOF.
        InputStream input = socket.getInputStream();
        String statusLine = readLine(input);
        int contentLength = -1;
        String connection = null;
        for (String line = readLine(input); !line.isEmpty(); line = readLine(input)) {
            String[] field = line.split(":", 2);
            assertEquals(2, field.length, "Invalid response header field");
            if (field[0].equalsIgnoreCase("Content-Length")) {
                assertEquals(-1, contentLength, "Response must contain only one Content-Length");
                contentLength = Integer.parseInt(field[1].trim());
            } else if (field[0].equalsIgnoreCase("Connection")) {
                connection = field[1].trim();
            }
        }
        assertTrue(contentLength >= 0, "Response must declare its body length");
        byte[] body = input.readNBytes(contentLength);
        assertEquals(contentLength, body.length, "Response body is incomplete");
        return new Response(statusLine, connection, new String(body, StandardCharsets.UTF_8));
    }

    private String readLine(InputStream input) throws Exception {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            int next = input.read();
            if (next == -1) {
                throw new EOFException("Connection closed before the response headers were complete");
            }
            if (next == '\n') {
                byte[] bytes = line.toByteArray();
                assertTrue(bytes.length > 0 && bytes[bytes.length - 1] == '\r',
                        "Response lines must end with CRLF");
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.US_ASCII);
            }
            line.write(next);
            assertTrue(line.size() <= 16 * 1024, "Response header line is too long");
        }
    }

    private record Response(String statusLine, String connection, String body) {
    }
}
