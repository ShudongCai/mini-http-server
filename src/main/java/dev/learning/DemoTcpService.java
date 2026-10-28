package dev.learning;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


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
            socket.setSoTimeout(5_000);

            InputStream input =  socket.getInputStream();
            System.out.println("Waiting for Data...");

            int data = input.read();

            System.out.println("Received: " + data);
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
