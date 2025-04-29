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
        executor.submit(
            () -> {
              try (BufferedReader in =
                      new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                  BufferedWriter out =
                      new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
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
                } catch (IOException ignore) {
                }
              }
            });
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendError(Socket clientSocket, String errorMessage) {}

  private void sendHttpResponse(BufferedWriter out, int statusCode, String statusText, String body)
      throws IOException {
    out.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
    out.write("Content-Type: text/plain\r\n");
    out.write("Content-Length: " + body.length() + "\r\n");
    out.write("\r\n");
    out.write(body);
    out.flush();
  }

  public void handleRequest(Socket clientSocket, Supplier<Integer> getNextServer) {
    try (Socket serverSocket = new Socket("localhost", getNextServer.get())) {
      serverSocket.setSoTimeout(5000);

      InputStream clientIn = clientSocket.getInputStream();
      OutputStream clientOut = clientSocket.getOutputStream();
      InputStream serverIn = serverSocket.getInputStream();
      OutputStream serverOut = serverSocket.getOutputStream();

      byte[] buffer = new byte[4096];
      int bytesRead;

      if ((bytesRead = clientIn.read(buffer)) != -1) {
        serverOut.write(buffer, 0, bytesRead);
        serverOut.flush();

        bytesRead = serverIn.read(buffer);
        if (bytesRead != -1) {
          clientOut.write(buffer, 0, bytesRead);
          clientOut.flush();
        } else {
          System.out.println("No response received from server");
        }
      } else {
        System.out.println("No data received from client");
      }
    } catch (Exception e) {
      sendError(clientSocket, "Error happened while handling request: " + e.getMessage());
      safeClose(clientSocket);
      throw new Error("Error handling request", e);
    } finally {
      safeClose(clientSocket);
    }
  }

  public boolean isServerHealthy(Integer serverPort) {
    try (Socket socket = new Socket("localhost", serverPort)) {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      out.write("GET /healthcheck HTTP/1.0\r\n");
      out.write("Host: localhost:" + serverPort + "\r\n");
      out.write("Connection: Close\r\n");
      out.write("\r\n");
      out.flush();

      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String statusLine = in.readLine();

      if (statusLine == null) {
        System.err.println("No response from server at port " + serverPort);
        return false;
      }

      boolean isSuccess = statusLine.contains("200 OK");

      if (!isSuccess) {
        System.err.println("Server responded with error: " + statusLine);
        return false;
      }

      String line;
      int contentLength = 0;
      while ((line = in.readLine()) != null && !line.isEmpty()) {
        if (line.toLowerCase().startsWith("content-length:")) {
          contentLength = Integer.parseInt(line.split(":")[1].trim());
        }
      }

      if (contentLength > 0) {
        char[] bodyChars = new char[contentLength];
        in.read(bodyChars);
        String body = new String(bodyChars).trim();

        if ("healthy".equals(body)) {
          System.out.println("Server on port " + serverPort + " is healthy");
          return true;
        }
      }

      System.err.println("Server on port " + serverPort + " is not healthy");
      return false;

    } catch (IOException e) {
      return false;
    }
  }

  private void safeClose(Socket socket) {
    if (socket != null && !socket.isClosed()) {
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }
}
