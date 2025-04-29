package br.imd.ufrn.gateway;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class UdpProtocol implements ProtocolHandler<DatagramPacket> {
  private final int port;
  private final ExecutorService executor;
  private final Supplier<Integer> getNextServer;
  private DatagramSocket serverSocket;

  public UdpProtocol(int port, ExecutorService executor, Supplier<Integer> getNextServer) {
    this.port = port;
    this.executor = executor;
    this.getNextServer = getNextServer;
  }

  public void startServer() {
    try {
      serverSocket = new DatagramSocket(null);
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(port));
      System.out.println("UDP server listening on port " + port);

      byte[] buffer = new byte[1024];

      while (true) {
        DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(requestPacket);

        byte[] requestData = Arrays.copyOf(requestPacket.getData(), requestPacket.getLength());
        DatagramPacket threadSafePacket = new DatagramPacket(
                requestData,
                requestData.length,
                requestPacket.getAddress(),
                requestPacket.getPort()
        );

        executor.submit(() -> handleRequest(threadSafePacket, getNextServer));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (serverSocket != null) serverSocket.close();
    }
  }

  @Override
  public void handleServerRegister(IntConsumer registerServer) {
    try (DatagramSocket socket = new DatagramSocket(8081)) {
      System.out.println("Listening for UDP server registration on port 8081");

      byte[] buffer = new byte[1024];
      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength());

        if (message.startsWith("register:")) {
          try {
            int port = Integer.parseInt(message.substring("register:".length()));
            registerServer.accept(port);
            System.out.println("Registered UDP server on port: " + port);
          } catch (NumberFormatException e) {
            System.err.println("Invalid UDP port: " + message);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Error in UDP register handler", e);
    }
  }

  @Override
  public void handleRequest(DatagramPacket clientPacket, Supplier<Integer> getNextServer) {
    try (DatagramSocket forwardSocket = new DatagramSocket()) {
      int nextPort = getNextServer.get();
      System.out.printf("Forwarding UDP packet to server on port %d%n", nextPort);

      forwardSocket.setSoTimeout(5000);
      forwardSocket.send(new DatagramPacket(
              clientPacket.getData(),
              clientPacket.getLength(),
              InetAddress.getByName("localhost"),
              nextPort
      ));

      byte[] responseBuffer = new byte[1024];
      DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
      forwardSocket.receive(responsePacket);

      try (DatagramSocket responseSocket = new DatagramSocket(null)) {
        responseSocket.setReuseAddress(true);
        responseSocket.bind(new InetSocketAddress(port));
        responseSocket.send(new DatagramPacket(
                responsePacket.getData(),
                responsePacket.getLength(),
                clientPacket.getAddress(),
                clientPacket.getPort()
        ));
      }
    } catch (SocketTimeoutException e) {
      sendError(clientPacket, "Server timeout");
    } catch (IOException e) {
      sendError(clientPacket, "Internal error");
    }
  }

  public boolean isServerHealthy(Integer port) {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(5000);

      String healthcheckMessage = "healthcheck";
      byte[] sendData = healthcheckMessage.getBytes();
      InetAddress serverAddress = InetAddress.getByName("localhost");

      DatagramPacket sendPacket =
          new DatagramPacket(sendData, sendData.length, serverAddress, port);
      socket.send(sendPacket);

      byte[] responseBuffer = new byte[1024];
      DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

      socket.receive(responsePacket);

      String response = new String(responsePacket.getData(), 0, responsePacket.getLength());

      return "healthy".equals(response);
    } catch (IOException e) {
      return false;
    }
  }

  public void sendError(DatagramPacket clientPacket, String errorMessage) {
    try (DatagramSocket errorSocket = new DatagramSocket(null)) {
      errorSocket.setReuseAddress(true);
      errorSocket.bind(new InetSocketAddress(port));
      byte[] errorData = errorMessage.getBytes();
      errorSocket.send(new DatagramPacket(
              errorData,
              errorData.length,
              clientPacket.getAddress(),
              clientPacket.getPort()
      ));
    } catch (IOException e) {
      System.err.println("Failed to send error: " + e.getMessage());
    }
  }

}
