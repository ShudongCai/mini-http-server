package dev.learning;

public class Main {
    public static void main(String[] args) {
        DemoTcpService demoTcpService = new DemoTcpService(8080);
        demoTcpService.listen();

        demoTcpService.bind();

        demoTcpService.close();
    }
}