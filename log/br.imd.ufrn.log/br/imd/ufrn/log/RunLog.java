package br.imd.ufrn.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class LogServer {
    private final int port;
    private final List<String> messageLog;
    private final ExecutorService executor;

    public LogServer(int port) {
        this.port = port;
        this.messageLog = Collections.synchronizedList(new ArrayList<>());
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port, 1000)) {
            System.out.println("Log Server started at port " + port);
            registerWithGateway();

            while (true) {
                Socket conn = serverSocket.accept();
                executor.submit(() -> handleConnection(conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("Server failed", e);
        }
    }

    private void registerWithGateway() {
        try (Socket socket = new Socket("localhost", 8081);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("register:" + port);
        } catch (Exception e) {
            System.err.println("Failed to register with gateway: " + e.getMessage());
        }
    }

    private void handleConnection(Socket conn) {
        try (conn;
             BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
             PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {

            String message = in.readLine();
            System.out.println("Received: " + message);

            if ("healthcheck".equals(message)) {
                out.println("healthy");
                return;
            }

            // Store the message in log
            synchronized (messageLog) {
                messageLog.add(message);
            }

            // Acknowledge receipt
            out.println("ACK");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getMessageLog() {
        return new ArrayList<>(messageLog);
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        new LogServer(port).start();
    }
}
