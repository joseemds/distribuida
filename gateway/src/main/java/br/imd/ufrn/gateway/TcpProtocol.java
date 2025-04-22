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
      e.printStackTrace();
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
}
