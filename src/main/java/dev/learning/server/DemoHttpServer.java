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

/**
 * This class implements a basic HTTP server.
 * The server waits for incoming HTTP requests over the network.
 *
 * <p>The server supports the following operations:
 * <ul>
 *      <li>Accepting TCP connections</li>
 *      <li>Routing HTTP requests to registered handlers</li>
 *      <li>Handling multiple requests on the same connection</li>
 * </ul>
 *
 * <p>Connections are handled synchronously on the calling thread;
 * this class does not create background worker threads.
 *
 * <p>Before sending a response, the server overwrites its Connection header
 * according to the connection policy. Handler-supplied Connection settings
 * do not control the socket lifecycle.
 */
public class DemoHttpServer {
    private ServerSocket serverSocket;
    private final HttpRouter router;
    private final int port;

    /**
     * Creates an HTTP server configured with the specified port and router.
     *
     * <p>The server socket is bound when {@link #listen()} is called.
     *
     * @param port the port on which the server will listen, or zero to let
     *             the operating system assign a port when the socket is bound
     * @param router the router used to dispatch decoded HTTP requests;
     *               must not be null
     */
    public DemoHttpServer(int port, HttpRouter router) {
        this.port = port;
        this.router = router;
    }

    /**
     * Creates a server socket bound to the configured port.
     *
     * <p>This method does not accept client connections.
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
     * Accepts one TCP connection and handles HTTP requests on that connection.
     *
     * <p>This method blocks while waiting for a connection and while handling it.
     * Multiple requests may be handled before the connection is closed.
     * The accepted socket is closed when handling finishes or fails.
     *
     * <p>After a normal response, the connection remains open unless the request
     * includes the Connection: close option. If the input ends between requests,
     * no further response is sent.
     *
     * <p>If request decoding throws {@link IllegalArgumentException} or
     * {@link EOFException}, the server attempts to send a 400 Bad Request response.
     * If decoding throws {@link UnsupportedOperationException}, it attempts to send
     * a 501 Not Implemented response. In either case, the connection is closed
     * after the response attempt.
     *
     * <p>Socket reads use a 30-second timeout. Socket read timeouts and other
     * I/O errors are logged rather than propagated to the caller. Runtime
     * exceptions from request handlers propagate to the caller and are not
     * converted into HTTP error responses.
     *
     * <p>The server must be listening before this method is called.
     */
    public void acceptAndRespondOnce() {
        try (Socket socket = serverSocket.accept()) {
            handleConnection(socket);
        } catch (SocketTimeoutException e) {
            System.out.println("Client read timeout");
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        }
    }

    /**
     * Closes the listening server socket.
     *
     * <p>This method does not close an already accepted client connection.
     * {@link #listen()} must be called successfully before this method.
     *
     * @throws RuntimeException if an I/O error occurs while closing
     *         the server socket
     */
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
