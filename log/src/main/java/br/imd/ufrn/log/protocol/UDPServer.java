package br.imd.ufrn.log.protocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

public class UDPServer extends AbstractServer {

  @Override
  public void run(int port, ExecutorService executor) {
    try (DatagramSocket socket = new DatagramSocket(port)) {
      System.out.println("UDP server started at port " + port);

      byte[] receiveData = new byte[1024];

      while (true) {
        // Receive a UDP packet
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        // Submit the packet processing task to the executor
        executor.submit(() -> handleUdpRequest(receivePacket, socket));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleUdpRequest(DatagramPacket receivePacket, DatagramSocket socket) {
    try {
      // Get message from the packet
      String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
      System.out.println("Received: " + message);

      // Check if the message is in key:value format
      String response;
      if (message != null && message.contains(":")) {
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
          // Log the message with key and value
          log(parts[0], parts[1]);
          response = "Logged " + parts[0] + ": " + parts[1];
        } else {
          response = "Invalid message format. Expected format: key:value";
        }
      } else {
        response = "Invalid message format. Expected format: key:value";
      }

      // Send response back to the client
      InetAddress clientAddress = receivePacket.getAddress();
      int clientPort = receivePacket.getPort();
      byte[] responseData = response.getBytes();
      DatagramPacket responsePacket =
          new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
      socket.send(responsePacket);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
