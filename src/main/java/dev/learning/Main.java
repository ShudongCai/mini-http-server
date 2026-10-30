package dev.learning;

import java.io.ByteArrayInputStream;
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

        HttpHeaderReader reader = new HttpHeaderReader();

        reader.readHeaders(stream);
//        DemoTcpService demoTcpService = new DemoTcpService(8080);
//        demoTcpService.listen();
//
//        demoTcpService.bind();
//
//        demoTcpService.close();
    }
}