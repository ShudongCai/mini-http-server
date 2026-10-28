package dev.learning;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;


public class DemoTcpService {
    private ServerSocket serverSocket;
    private final int port;

    public DemoTcpService(int port) {
        this.port = port;
    }

    public void listen() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void bind() {
        try(Socket socket = serverSocket.accept()) {
            OutputStream output = socket.getOutputStream();
            String content =
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\n" +
                    "Content-Length: 12\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "Hello World!";
            output.write(content.getBytes(StandardCharsets.UTF_8));
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
