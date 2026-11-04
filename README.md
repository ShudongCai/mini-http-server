# Mini HTTP Server

A small HTTP/1.1 server built from scratch in **Java 21** to explore TCP networking, HTTP request handling, and concurrency without using a web framework.

## Features

- TCP connections using `ServerSocket` and `Socket`
- HTTP request parsing, including headers and `Content-Length` bodies
- Simple method/path routing (GET and POST)
- HTTP/1.1 keep-alive connections
- Concurrent connections using Java 21 virtual threads
- Connection limits using `Semaphore`
- Graceful shutdown with an executor termination timeout
- JUnit tests for parsing, routing, and server behavior

## Requirements

- Java 21
- Maven

## Run

```bash
mvn clean package
java -cp target/classes dev.learning.Main
```

Run the tests with:

```bash
mvn test
```

Routes are registered in `Main.java`.

## Scope

This is an educational project, **not a production-ready HTTP server**. It intentionally implements a limited subset of HTTP/1.1; for example, chunked request bodies are not supported.
