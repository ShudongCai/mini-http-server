package dev.learning;

import dev.learning.response.HttpResponse;
import dev.learning.router.HttpRouter;
import dev.learning.server.DemoHttpServer;

import java.nio.charset.StandardCharsets;

/**
 * Entry point that registers demo routes and accepts connections on port 8080.
 * Each connection is handled asynchronously on its own virtual thread.
 *
 * <p>The accept loop checks {@link DemoHttpServer#isRunning()}. A JVM shutdown hook
 * calls {@link DemoHttpServer#close()} to stop accepting connections and apply the
 * server's grace period before interrupting unfinished connection tasks.
 */
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

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    System.out.println("Shutting down HTTP server...");
                    demoHttpServer.close();
                    System.out.println("HTTP server shutdown completed.");
                }, "http-server-shutdown")
        );

        while (demoHttpServer.isRunning()) {
            demoHttpServer.acceptAndRespondOnce();
        }

    }
}