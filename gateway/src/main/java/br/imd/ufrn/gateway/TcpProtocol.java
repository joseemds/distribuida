package br.imd.ufrn.gateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class TcpProtocol implements ProtocolHandler<Socket> {
  private final int port;
  private final ExecutorService executor;
  private final Supplier<Integer> getNextServer;

  TcpProtocol(int port, ExecutorService executor, Supplier<Integer> getNextServer) {
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
      throw new RuntimeException(e);
    }
  }

  @Override
  public void handleServerRegister(IntConsumer registerServer) {
    try (ServerSocket socket = new ServerSocket(8081); ) {
      while (true) {

        Socket clientSocket = socket.accept();
        BufferedReader in =
            new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String message = in.readLine();

        if (message != null && message.startsWith("register:")) {
          String portStr = message.substring("register:".length());
          try {
            int port = Integer.parseInt(portStr);
            registerServer.accept(port);
            System.out.println("Registered server on port: " + port);
          } catch (NumberFormatException e) {
            System.err.println("Invalid port format: " + portStr);
          }
        }

        in.close();
        clientSocket.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  public boolean isServerHealthy(Integer port) {
    try (Socket socket = new Socket("localhost", port)) {
      socket.setSoTimeout(2000);

      OutputStream outputStream = socket.getOutputStream();
      PrintWriter writer = new PrintWriter(outputStream, true);
      writer.println("healthcheck");

      InputStream inputStream = socket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String response = reader.readLine();

      return "healthy".equals(response);
    } catch (IOException e) {
      return false;
    }
  }

  public void sendError(Socket clientSocket, String errorMessage) {
    try {
      OutputStream outputStream = clientSocket.getOutputStream();
      PrintWriter writer = new PrintWriter(outputStream, true);
      writer.println("ERROR: " + errorMessage);
    } catch (IOException ignored) {
    } finally {
      try {
        clientSocket.close();
      } catch (IOException ignored) {
      }
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
