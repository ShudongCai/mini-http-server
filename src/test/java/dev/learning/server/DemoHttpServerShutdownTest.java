package dev.learning.server;

import dev.learning.response.HttpResponse;
import dev.learning.router.HttpRouter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class DemoHttpServerShutdownTest {
    private DemoHttpServer server;
    private ServerSocket listener;
    private ExecutorService connections;
    private FutureTask<Void> acceptLoop;
    private Thread acceptThread;
    private FutureTask<Void> shutdown;
    private Socket client;
    private CountDownLatch finishHandler;

    @Test
    void shouldExitAcceptLoopWhenClosedDuringAccept() throws Exception {
        startServer(new HttpRouter());
        // Observe the actual accept call instead of relying on a fixed startup delay.
        awaitCondition(() -> Arrays.stream(acceptThread.getStackTrace()).anyMatch(frame ->
                frame.getClassName().equals(ServerSocket.class.getName())
                        && frame.getMethodName().equals("accept")), "Accept loop did not enter accept()");

        beginShutdown();

        shutdown.get(5, TimeUnit.SECONDS);
        acceptLoop.get(5, TimeUnit.SECONDS);
        assertFalse(server.isRunning());
        assertTrue(listener.isClosed());
        assertTrue(connections.isTerminated());
    }

    @Test
    void shouldCompleteCurrentRequestWithinGracePeriod() throws Exception {
        CountDownLatch handlerStarted = new CountDownLatch(1);
        finishHandler = new CountDownLatch(1);
        HttpRouter router = new HttpRouter();
        router.register("GET", "/slow", request -> {
            handlerStarted.countDown();
            try {
                if (!finishHandler.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Handler was not released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Handler interrupted before completing", e);
            }
            return HttpResponse.builder().setContent("Completed").build();
        });
        startServer(router);
        connectAndSend("/slow");
        assertTrue(handlerStarted.await(5, TimeUnit.SECONDS), "Handler did not start");

        beginShutdown();
        awaitCondition(connections::isShutdown, "Executor shutdown did not start");
        assertFalse(shutdown.isDone(), "Shutdown must wait for the current request");
        finishHandler.countDown();

        Response response = readResponse(client.getInputStream());
        assertEquals("HTTP/1.1 200 OK", response.statusLine());
        assertEquals("Completed", response.body());
        assertEquals("close", response.connection());
        assertEquals(-1, client.getInputStream().read());
        shutdown.get(5, TimeUnit.SECONDS);
        acceptLoop.get(5, TimeUnit.SECONDS);
        assertTrue(connections.isTerminated());
    }

    @Test
    void shouldInterruptIdleKeepAliveConnectionAfterGracePeriod() throws Exception {
        HttpRouter router = new HttpRouter();
        router.register("GET", "/hello", request ->
                HttpResponse.builder().setContent("Hello").build());
        startServer(router);
        connectAndSend("/hello");
        Response response = readResponse(client.getInputStream());
        assertEquals("HTTP/1.1 200 OK", response.statusLine());
        assertEquals("Hello", response.body());
        assertEquals("keep-alive", response.connection());

        long shutdownStarted = System.nanoTime();
        beginShutdown();
        awaitCondition(connections::isShutdown, "Executor shutdown did not start");
        assertThrows(TimeoutException.class, () -> shutdown.get(200, TimeUnit.MILLISECONDS),
                "Idle connections must be given a grace period before interruption");

        try {
            assertEquals(-1, client.getInputStream().read(), "Idle connection must be closed");
        } catch (SocketException expected) {
            // Forced cleanup may be observed as EOF or a TCP reset, depending on the platform.
        }
        assertTrue(System.nanoTime() - shutdownStarted >= TimeUnit.SECONDS.toNanos(10),
                "Idle connection was closed before the configured 10-second grace period");
        shutdown.get(5, TimeUnit.SECONDS);
        acceptLoop.get(5, TimeUnit.SECONDS);
        assertFalse(server.isRunning());
        assertTrue(listener.isClosed());
        assertTrue(connections.isTerminated(), "Connection tasks must be cleaned up");
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (finishHandler != null) {
            finishHandler.countDown();
        }
        try {
            if (client != null) {
                client.close();
            }
        } finally {
            if (server != null) {
                if (shutdown == null) {
                    beginShutdown();
                }
                shutdown.get(15, TimeUnit.SECONDS);
                acceptLoop.get(5, TimeUnit.SECONDS);
                acceptThread.join(5000);
                assertFalse(acceptThread.isAlive(), "Accept thread did not stop");
                assertTrue(connections.awaitTermination(5, TimeUnit.SECONDS),
                        "Connection tasks did not stop");
            }
        }
    }

    private void startServer(HttpRouter router) throws Exception {
        DemoHttpServer startedServer = new DemoHttpServer(0, router);
        startedServer.listen();
        server = startedServer;
        // Existing server APIs do not expose the assigned port or executor termination.
        listener = (ServerSocket) serverField("serverSocket");
        connections = (ExecutorService) serverField("executorService");
        acceptLoop = new FutureTask<>(() -> {
            while (server.isRunning()) {
                server.acceptAndRespondOnce();
            }
        }, null);
        acceptThread = Thread.ofPlatform().daemon(true).start(acceptLoop);
    }

    private Object serverField(String name) throws Exception {
        Field field = DemoHttpServer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(server);
    }

    private void connectAndSend(String path) throws Exception {
        client = new Socket();
        client.connect(new InetSocketAddress("127.0.0.1", listener.getLocalPort()), 5000);
        client.setSoTimeout(15_000);
        client.getOutputStream().write(
                ("GET " + path + " HTTP/1.1\r\nHost: localhost\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
        client.getOutputStream().flush();
    }

    private void beginShutdown() {
        shutdown = new FutureTask<>(server::close, null);
        Thread.ofPlatform().daemon(true).start(shutdown);
    }

    private void awaitCondition(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, message);
            if (Thread.interrupted()) {
                throw new InterruptedException(message);
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }

    private Response readResponse(InputStream input) throws Exception {
        String statusLine = readLine(input);
        int length = -1;
        String connection = null;
        for (String line = readLine(input); !line.isEmpty(); line = readLine(input)) {
            String[] field = line.split(":", 2);
            assertEquals(2, field.length);
            if (field[0].equalsIgnoreCase("Content-Length")) {
                length = Integer.parseInt(field[1].trim());
            } else if (field[0].equalsIgnoreCase("Connection")) {
                connection = field[1].trim();
            }
        }
        assertTrue(length >= 0, "Response must have a Content-Length");
        byte[] body = input.readNBytes(length);
        assertEquals(length, body.length, "Incomplete response body");
        return new Response(statusLine, connection, new String(body, StandardCharsets.UTF_8));
    }

    private String readLine(InputStream input) throws Exception {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        for (int next; (next = input.read()) != -1;) {
            if (next == '\n') {
                byte[] bytes = line.toByteArray();
                assertTrue(bytes.length > 0 && bytes[bytes.length - 1] == '\r');
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.US_ASCII);
            }
            line.write(next);
            assertTrue(line.size() <= 16 * 1024, "Response header line is too long");
        }
        throw new EOFException("Connection closed before completing response headers");
    }

    private record Response(String statusLine, String connection, String body) {
    }
}
