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


public class DemoHttpServer {
    private ServerSocket serverSocket;
    private final HttpRouter router;
    private final int port;

    public DemoHttpServer(int port, HttpRouter router) {
        this.port = port;
        this.router = router;
    }

    public void listen() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void acceptAndRespondOnce() {
        try (Socket socket = serverSocket.accept()) {
            handleConnection(socket);
        }
        catch (SocketTimeoutException e) {
            System.out.println("Client read timeout");
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        }
    }

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
