package br.imd.ufrn.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.server.protocol.ProtocolParser;
import br.imd.ufrn.server.protocol.action.*;
import br.imd.ufrn.server.protocol.action.Action;

public class UDPServer implements Server {
  private final ProtocolParser parser = new ProtocolParser();
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

        executor.submit(() -> handleRequest(socket, packet));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void handleRequest(DatagramSocket socket, DatagramPacket packet) {
    try {
      String message = new String(packet.getData(), 0, packet.getLength()).trim();
      System.out.println("Received: " + message);

      Action action = parser.parse(message);

      String response = switch (action) {
        case Create create -> "Document " + create.documentName() + " created";
        case Edit edit -> "Edited " + edit.documentName() + " to " + edit.content();
        case Get get -> "Document " + get.documentName() + " content is: ";
      };

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

  private void sendRegister(int port) {
    try (DatagramSocket socket = new DatagramSocket()) {
      String message = "register:" + port;
      byte[] buffer = message.getBytes();
      InetAddress address = InetAddress.getByName("localhost");
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 8081);
      socket.send(packet);
    } catch (Exception e) {
      throw new RuntimeException("Error when sending register", e);
    }
  }
}
