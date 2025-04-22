package br.imd.ufrn.gateway;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class UdpProtocol implements ProtocolHandler<DatagramPacket> {
    private final int port;
    private final ExecutorService executor;
    private final Supplier<Integer> getNextServer;

    public UdpProtocol(int port, ExecutorService executor, Supplier<Integer> getNextServer) {
        this.port = port;
        this.executor = executor;
        this.getNextServer = getNextServer;
    }

    public void startServer() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP server listening on port " + port);

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);

                executor.submit(() -> handleRequest(requestPacket, getNextServer));
            }

        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println("Received: " + message);

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

    public void handleRequest(DatagramPacket clientPacket, Supplier<Integer> getNextServer) {
        int nextPort = getNextServer.get();
        String message = new String(clientPacket.getData(), 0, clientPacket.getLength());
        System.out.printf("Forwarding UDP packet to server on port %d: %s%n", nextPort, message);

        try (DatagramSocket forwardSocket = new DatagramSocket()) {
            byte[] sendData = message.getBytes();
            InetAddress localhost = InetAddress.getByName("localhost");

            DatagramPacket forwardPacket = new DatagramPacket(sendData, sendData.length, localhost, nextPort);
            forwardSocket.send(forwardPacket);

            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            forwardSocket.receive(responsePacket);

            DatagramSocket replySocket = new DatagramSocket();
            DatagramPacket reply = new DatagramPacket(
                    responsePacket.getData(),
                    responsePacket.getLength(),
                    clientPacket.getAddress(),
                    clientPacket.getPort()
            );
            replySocket.send(reply);
            replySocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isServerHealthy(Integer port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);

            String healthcheckMessage = "healthcheck";
            byte[] sendData = healthcheckMessage.getBytes();
            InetAddress serverAddress = InetAddress.getByName("localhost");

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
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


    private void pipeBidirectional(Socket socketA, Socket socketB, ExecutorService executor) {
        try {
            InputStream inputA = socketA.getInputStream();
            OutputStream outputA = socketA.getOutputStream();
            InputStream inputB = socketB.getInputStream();
            OutputStream outputB = socketB.getOutputStream();

            executor.submit(() -> {
                try {
                    inputA.transferTo(outputB);
                    socketB.shutdownOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            executor.submit(() -> {
                try {
                    inputB.transferTo(outputA);
                    socketA.shutdownOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException e) {
            throw new RuntimeException("Failed to establish bidirectional pipe", e);
        }
    }

}
