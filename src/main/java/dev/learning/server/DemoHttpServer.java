package dev.learning.server;

import dev.learning.response.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;


public class DemoHttpServer {
    private ServerSocket serverSocket;
    private final int port;

    public DemoHttpServer(int port) {
        this.port = port;
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
            OutputStream output = socket.getOutputStream();
            HttpResponse httpHeader = HttpResponse.builder().setContent("HelloWorld! HI! Hi!").build();
            System.out.println(httpHeader.toHttpString());
            output.write(httpHeader.toHttpString().getBytes(StandardCharsets.UTF_8));
        } catch (SocketTimeoutException e) {
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




}
