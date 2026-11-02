package dev.learning;

import dev.learning.response.HttpResponse;
import dev.learning.router.HttpRouter;
import dev.learning.server.DemoHttpServer;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        HttpRouter router = new HttpRouter();

        router.register("POST", "/echo", request -> {
            String body = new String(
                    request.getHttpBody(),
                    StandardCharsets.UTF_8
            );

            return HttpResponse.builder()
                    .setContent("You sent: " + body)
                    .build();
        });

        router.register("GET", "/hello", request ->
                HttpResponse.builder()
                        .setContent("Hello World")
                        .build()
        );

        router.register("POST", "/hello", request ->
                HttpResponse.builder()
                        .setContent("Hello POST")
                        .build()
        );

        DemoHttpServer demoHttpServer = new DemoHttpServer(8080, router);
        demoHttpServer.listen();

        while (true) {
            demoHttpServer.acceptAndRespondOnce();
        }

    }
}