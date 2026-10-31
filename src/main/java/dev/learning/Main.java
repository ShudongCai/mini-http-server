package dev.learning;

import dev.learning.request.HttpRequestHeadReader;
import dev.learning.server.DemoHttpServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        ByteArrayInputStream stream = new ByteArrayInputStream(
                ("GET /a HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "\r\n" +
                        "GET /b HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "\r\n" +
                        "GET /c").getBytes(StandardCharsets.UTF_8)
        );

        HttpRequestHeadReader reader = new HttpRequestHeadReader();

        try {
            reader.readHead(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        DemoHttpServer demoHttpServer = new DemoHttpServer(8080);
//        demoHttpServer.listen();
//
//        demoHttpServer.acceptAndRespondOnce();
//
//        demoHttpServer.close();
    }
}