package br.imd.ufrn.log.protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class HttpServer extends AbstractServer {

  @Override
  public void run(int port, ExecutorService executor) {
    try (ServerSocket serverSocket = new ServerSocket(port, 1000)) {
      System.out.println("HTTP server started at port " + port);

      while (true) {
        Socket conn = serverSocket.accept();
        executor.submit(() -> handleHttpRequest(conn));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void handleHttpRequest(Socket conn) {
    try (conn;
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        OutputStream out = conn.getOutputStream()) {

      String requestLine = in.readLine();
      System.out.println("Request: " + requestLine);

      if (requestLine != null && requestLine.startsWith("POST /log")) {

        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {}

        StringBuilder body = new StringBuilder();
        while (in.ready()) {
          body.append((char) in.read());
        }

        String message = body.toString().trim();
        System.out.println("Received body: " + message);

        String response;
        if (message != null && message.contains(":")) {
          String[] parts = message.split(":", 2);
          if (parts.length == 2) {
            log(parts[0], parts[1]);
            response = "Logged " + parts[0] + ": " + parts[1];
          } else {
            response = "Invalid message format. Expected format: key:value";
          }
        } else {
          response = "Invalid message format. Expected format: key:value";
        }

        String httpResponse =
            "HTTP/1.0 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: "
                + response.length()
                + "\r\n"
                + "\r\n"
                + response;
        out.write(httpResponse.getBytes());
        out.flush();

      } else {
        String httpResponse = "HTTP/1.0 404 Not Found\r\n\r\n";
        out.write(httpResponse.getBytes());
        out.flush();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
