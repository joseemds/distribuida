package br.imd.ufrn.log.protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TCPServer extends AbstractServer {

  @Override
  public void run(int port, ExecutorService executor) {
    try (ServerSocket serverSocket = new ServerSocket(port, 1000)) {
      System.out.println("TCP server started at port " + port);

      while (true) {
        Socket conn = serverSocket.accept();
        executor.submit(() -> handleTcpRequest(conn));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void handleTcpRequest(Socket conn) {
    try (conn;
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {

      String message = in.readLine();
      System.out.println("Received: " + message);
      if (message != null && message.contains(":")) {
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
          log(parts[0], parts[1]);
          out.println("Logged  " + parts[0] + ": " + parts[1]);
        }
      } else {
        out.println("Logged nothing, message invalid: " + message);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
