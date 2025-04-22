package br.imd.ufrn.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.server.protocol.action.Action;
import br.imd.ufrn.server.versionvector.VersionedDocument;

public class TCPServer extends AbstractServer {

  @Override
  public void run(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
         ServerSocket serverSocket = new ServerSocket(port, 1000)) {
      System.out.println("TCP server started at port " + port);
      sendRegister(port);

      while (true) {
        Socket conn = serverSocket.accept();
        executor.submit(() -> handleTcpRequest(conn));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void sendRegister(int port) {
    try (Socket socket = new Socket("localhost", 8081);
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

      String message = "register:" + port;
      out.println(message);

    } catch (Exception e) {
      throw new RuntimeException("Error when sending TCP register", e);
    }
  }

  private void handleTcpRequest(Socket conn) {
    try (conn;
         BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
         PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {

      String message = in.readLine();
      System.out.println("Received: " + message);

      if ("healthcheck".equals(message)) {
        System.out.println("Healthcheck received, sending healthy response.");
        out.println("healthy");
        return;
      }

      Action action = parser.parse(message);
      String response = handleAction(action);

      out.println(response);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  protected void propagateChanges(VersionedDocument versionedDoc) {
    try (Socket gatewaySocket = new Socket("localhost", 8081);
         PrintWriter out = new PrintWriter(gatewaySocket.getOutputStream(), true)) {

      String message = "sync:" + versionedDoc.getContent() + ":" + versionedDoc.getVersionVector().getVersions();
      out.println(message);


    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error propagating changes to the gateway", e);
    }
  }


}