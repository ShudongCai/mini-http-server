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
        try(Socket socket = serverSocket.accept()) {
            HttpRequestDecoder decoder = new HttpRequestDecoder();
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
            HttpResponse resposne;

            try {
                HttpRequest request = decoder.decode(input);

                if (request == null) {
                    System.out.println("Client closed connection without sending a request");
                    return;
                }

                System.out.println(
                        request.getHttpRequestHeader().requestLine().method()
                                + " " + request.getHttpRequestHeader().requestLine().target()
                                + " " + request.getHttpRequestHeader().requestLine().version()
                );
                printHeadersAndContents(request.getHttpRequestHeader().headerFields().getHeaders());
                System.out.println(
                        new String(request.getHttpBody(), StandardCharsets.UTF_8)
                );

                resposne = router.route(request);
            } catch(IllegalArgumentException | EOFException e) {
                resposne = HttpResponse.builder()
                        .setStatusCode(400)
                        .setReasonPhrase("Bad Request")
                        .setContent("Bad Request")
                        .build();
            } catch (UnsupportedOperationException e) {
                resposne = HttpResponse.builder()
                        .setStatusCode(501)
                        .setReasonPhrase("Not Implemented")
                        .setContent("Not Implemented")
                        .build();
            }
            output.write(resposne.toHttpString().getBytes(StandardCharsets.UTF_8));
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
}
