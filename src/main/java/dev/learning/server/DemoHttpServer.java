package dev.learning.server;

import dev.learning.request.HttpRequestDecoder;
import dev.learning.request.model.HttpRequest;
import dev.learning.response.HttpResponse;
import dev.learning.router.HttpRouter;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Implements a basic HTTP server with concurrent connection handling.
 *
 * <p>The calling thread accepts one TCP connection per call to
 * {@link #acceptAndRespondOnce()} and submits it to an owned pool of four workers.
 * Submitted connection tasks wait in the executor queue when all workers are busy.
 * Each worker handles requests on its connection sequentially, using a separate
 * decoder for that connection, and may handle multiple requests before closing it.
 *
 * <p>Configure the shared router before accepting connections. Handlers may run
 * concurrently for different connections and must support concurrent invocation.
 * Before sending a response, the server overwrites its Connection header according
 * to the connection policy. Handlers must not share the same mutable response
 * instance across concurrent requests; handler-supplied Connection settings do not
 * control the socket lifecycle.
 */
public class DemoHttpServer {
    private ServerSocket serverSocket;
    private final HttpRouter router;
    private final int port;
    private final ExecutorService executorService;

    /**
     * Creates an HTTP server configured with the specified port and shared router.
     *
     * <p>The constructor creates the executor; worker threads are started as tasks
     * are submitted. The server socket is bound when {@link #listen()} is called.
     *
     * @param port the port on which the server will listen, or zero to let
     *             the operating system assign a port when the socket is bound
     * @param router the router shared by connection workers; must not be null and
     *               should be configured before accepting connections
     */
    public DemoHttpServer(int port, HttpRouter router) {
        this.port = port;
        this.router = router;
        executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Creates a server socket bound to the configured port.
     *
     * <p>This method does not accept client connections or start an accept loop.
     *
     * @throws RuntimeException if an I/O error occurs while creating
     *         or binding the server socket
     */
    public void listen() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Accepts one TCP connection and submits it for asynchronous handling.
     *
     * <p>This method blocks while waiting for a connection, then returns after task
     * submission without waiting for request handling to finish. A worker handles
     * requests on that connection sequentially and closes the accepted socket when
     * handling finishes or fails. If submission is rejected, this method attempts to
     * close the accepted socket before propagating the rejection.
     *
     * <p>After a normal response, the connection remains open unless the request
     * includes the Connection: close option. If the input ends between requests,
     * no further response is sent.
     *
     * <p>If request decoding throws {@link IllegalArgumentException} or
     * {@link EOFException}, the worker attempts to send a 400 Bad Request response.
     * If decoding throws {@link UnsupportedOperationException}, it attempts to send
     * a 501 Not Implemented response. In either case, the connection is closed
     * after the response attempt.
     *
     * <p>Socket reads use a 30-second timeout once a worker starts handling the
     * connection; this does not limit time spent waiting in the executor queue.
     * I/O errors during acceptance or worker execution, including socket read
     * timeouts, are logged rather than propagated to the accept caller.
     * Runtime exceptions from handlers are captured in the submitted task's Future;
     * this class does not inspect that Future or convert those failures into HTTP
     * error responses.
     *
     * <p>The server must be listening and the executor must not have been shut down.
     *
     * @throws RejectedExecutionException if the executor rejects the connection task,
     *         for example after shutdown
     */
    public void acceptAndRespondOnce() {
        try {
            Socket socket = serverSocket.accept();
            submitConnection(socket);
        } catch (SocketTimeoutException e) {
            System.out.println("Client read timeout");
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        };
    }

    private void submitConnection(Socket socket) throws IOException{
        try {
            executorService.submit(() -> {
                try(socket) {
                    handleConnection(socket);
                } catch (IOException e) {
                    System.out.println("I/O error: " + e.getMessage());
                }
            });
        } catch(RejectedExecutionException e) {
            try {
                socket.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    /**
     * Closes the listening server socket and initiates an orderly executor shutdown.
     *
     * <p>No new connection tasks are accepted by the executor after shutdown, but
     * previously submitted tasks, including queued tasks, are allowed to finish.
     * This method does not wait for them or directly close their client sockets.
     * Each worker remains responsible for closing the connection it handles.
     *
     * <p>The executor is shut down even if closing the listening socket fails.
     * {@link #listen()} must be called successfully before this method. Calling
     * listen again does not restart the executor after shutdown.
     *
     * @throws RuntimeException if an I/O error occurs while closing
     *         the server socket
     */
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
    }

    private void printHeadersAndContents(Map<String, List<String>> input) {
        for (Map.Entry<String, List<String>> entry : input.entrySet()) {
            String key = entry.getKey();
            List<String> list = entry.getValue();

            for (String content : list) {
                System.out.println(
                        key + ": " + content
                );
            }
        }
    }

    private void handleConnection(Socket socket) throws IOException {
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        socket.setSoTimeout(30_000);

        while (true) {
            HttpRequest request;
            try {
                request = decoder.decode(input);

                if (request == null) {
                    System.out.println("Client closed connection");
                    return;
                }
            } catch (IllegalArgumentException | EOFException e) {
                writeResponse(
                        output,
                        errorResponse(400, "Bad Request"),
                        true
                );
                return;
            } catch (UnsupportedOperationException e) {
                writeResponse(
                        output,
                        errorResponse(501, "Not Implemented"),
                        true
                );
                return;
            }

            logRequest(request);

            boolean close = hasCloseOption(request);
            HttpResponse response = handleRequest(request);
            writeResponse(output, response, close);

            if (close) {
                return;
            }
        }
    }

    private HttpResponse handleRequest(HttpRequest request) {
        return this.router.route(request);
    }

    private HttpResponse errorResponse(int statusCode, String content) {
        return HttpResponse.builder()
                .setStatusCode(statusCode)
                .setReasonPhrase(content)
                .setContent(content)
                .setConnection("close")
                .build();
    }

    private void writeResponse(OutputStream output, HttpResponse response, boolean close) throws IOException {
        response.setConnection(
                close ? "close" : "keep-alive"
        );

        output.write(response.toHttpString().getBytes(StandardCharsets.UTF_8));
    }

    private void logRequest(HttpRequest request) {
        System.out.println(
                request.getHttpRequestHeader().requestLine().method()
                        + " " + request.getHttpRequestHeader().requestLine().target()
                        + " " + request.getHttpRequestHeader().requestLine().version()
        );
        printHeadersAndContents(request.getHttpRequestHeader().headerFields().getHeaders());
        System.out.println(
                new String(request.getHttpBody(), StandardCharsets.UTF_8)
        );
    }

    private boolean hasCloseOption(HttpRequest request) {
        List<String> connections =
                request.getHttpRequestHeader().headerFields().get("connection");

        for (String connection : connections) {
            String[] connectionValues = connection.split(",");

            for (String value : connectionValues) {
                if (value.trim().equalsIgnoreCase("close")) {
                    return true;
                }
            }

        }

        return false;
    }
}
