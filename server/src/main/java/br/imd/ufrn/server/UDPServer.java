package br.imd.ufrn.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.server.protocol.action.Action;
import br.imd.ufrn.server.versionvector.VersionedDocument;

public class UDPServer extends AbstractServer {
  private static final int BUFFER_SIZE = 1024;

  @Override
  public void run(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
         DatagramSocket socket = new DatagramSocket(port)) {
      System.out.println("UDP server started at port " + port);
      sendRegister(port);

      byte[] buffer = new byte[BUFFER_SIZE];

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        executor.submit(() -> handleUdpRequest(socket, packet));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void sendRegister(int port) {
    try (DatagramSocket socket = new DatagramSocket()) {
      String message = "register:" + port;
      byte[] buffer = message.getBytes();
      InetAddress address = InetAddress.getByName("localhost");
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 8081);
      socket.send(packet);
    } catch (Exception e) {
      throw new RuntimeException("Error when sending UDP register", e);
    }
  }

  private void handleUdpRequest(DatagramSocket socket, DatagramPacket packet) {
    try {
      String message = new String(packet.getData(), 0, packet.getLength()).trim();
      System.out.println("Received: " + message);

      if ("healthcheck".equals(message)) {
        System.out.println("Healthcheck received, sending healthy response.");
        byte[] responseBytes = "healthy".getBytes();
        DatagramPacket responsePacket = new DatagramPacket(
                responseBytes,
                responseBytes.length,
                packet.getAddress(),
                packet.getPort()
        );
        socket.send(responsePacket);
        return;
      }

      Action action = parser.parse(message);
      String response = handleAction(action);

      byte[] responseBytes = response.getBytes();
      DatagramPacket responsePacket = new DatagramPacket(
              responseBytes,
              responseBytes.length,
              packet.getAddress(),
              packet.getPort()
      );
      socket.send(responsePacket);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void propagateChanges(VersionedDocument versionedDoc) {
    try (DatagramSocket socket = new DatagramSocket()) {
      String message = "sync:" + versionedDoc.getContent() + ":" + versionedDoc.getVersionVector().getVersions().toString();

      // Send the message to the API Gateway (assuming localhost:8081)
      InetAddress address = InetAddress.getByName("localhost");
      byte[] buffer = message.getBytes();
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 8081);
      socket.send(packet);

      System.out.println("Sent update to API Gateway: " + message);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error propagating changes to the gateway via UDP", e);
    }
  }
}