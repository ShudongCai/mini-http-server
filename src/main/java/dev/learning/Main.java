package dev.learning;

import dev.learning.server.DemoHttpServer;

public class Main {
    public static void main(String[] args) {
        DemoHttpServer demoHttpServer = new DemoHttpServer(8080);
        demoHttpServer.listen();

        while (true) {
            demoHttpServer.acceptAndRespondOnce();
        }

    }
}