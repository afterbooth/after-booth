package com.afterbooth.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A local stand-in for the Meta API on 127.0.0.1 that records every request it receives. */
final class StubMetaServer implements AutoCloseable {

  record Received(
      String method, String path, String authorization, String contentType, byte[] body) {

    String bodyText() {
      return new String(body, StandardCharsets.UTF_8);
    }
  }

  private final HttpServer server;
  private final List<Received> received = new CopyOnWriteArrayList<>();
  private volatile int status = 200;
  private volatile String response = "{}";

  StubMetaServer() {
    try {
      server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    server.createContext("/", this::handle);
    server.start();
  }

  StubMetaServer respond(int status, String response) {
    this.status = status;
    this.response = response;
    return this;
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  List<Received> received() {
    return received;
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private void handle(HttpExchange exchange) throws IOException {
    received.add(
        new Received(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst("Authorization"),
            exchange.getRequestHeaders().getFirst("Content-Type"),
            exchange.getRequestBody().readAllBytes()));
    byte[] body = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(body);
    }
  }
}
