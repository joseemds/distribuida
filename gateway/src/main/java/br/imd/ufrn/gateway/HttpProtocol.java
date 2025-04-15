package br.imd.ufrn.gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class HttpProtocol implements ProtocolHandler<Socket> {
  private final int port;
  private final ExecutorService executor;
  private final Supplier<Integer> getNextServer;

  HttpProtocol(int port, ExecutorService executor, Supplier<Integer> getNextServer) {
    this.port = port;
    this.executor = executor;
    this.getNextServer = getNextServer;
  }

  public void startServer() {
    try (ServerSocket serverSocket = new ServerSocket(this.port, 300)) {
      while (true) {
        Socket conn = serverSocket.accept();
        executor.submit(() -> this.handleRequest(conn, this.getNextServer));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void handleServerRegister(IntConsumer registerServer) {
    try (ServerSocket socket = new ServerSocket(8081)) {
      System.out.println("Listening for HTTP /register on port 8081");

      while (true) {
        Socket clientSocket = socket.accept();
        executor.submit(() -> {
          try (
                  BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                  BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
          ) {
            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("POST /register")) {
              sendHttpResponse(out, 400, "Bad Request", "Invalid endpoint");
              return;
            }

            String line;
            int contentLength = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
              if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
              }
            }

            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            String body = new String(bodyChars).trim();

            try {
              int port = Integer.parseInt(body);
              registerServer.accept(port);
              System.out.println("Registered HTTP server on port: " + port);
              sendHttpResponse(out, 200, "OK", "Registered server on port " + port);
            } catch (NumberFormatException e) {
              sendHttpResponse(out, 400, "Bad Request", "Invalid port format");
            }

          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            try {
              clientSocket.close();
            } catch (IOException ignore) {}
          }
        });
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendHttpResponse(BufferedWriter out, int statusCode, String statusText, String body) throws IOException {
    out.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
    out.write("Content-Type: text/plain\r\n");
    out.write("Content-Length: " + body.length() + "\r\n");
    out.write("\r\n");
    out.write(body);
    out.flush();
  }

  public void handleRequest(Socket clientSocket, Supplier<Integer> getNextServer) {
    int nextPort = this.getNextServer.get();

    try {

      Socket serverSocket = new Socket("localhost", nextPort);
      System.out.println("Redirecting to server on port " + nextPort);

      InputStream clientInput = clientSocket.getInputStream();
      OutputStream clientOutput = serverSocket.getOutputStream();

      InputStream serverInput = serverSocket.getInputStream();
      OutputStream serverOutput = serverSocket.getOutputStream();
      serverOutput.flush();

      clientInput.transferTo(serverOutput);
      serverInput.transferTo(clientOutput);
      clientOutput.flush();

      serverSocket.close();
      clientSocket.close();

    } catch (Exception e) {
      throw new Error("Error handling request", e);
    }
  }
}
