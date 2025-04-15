package br.imd.ufrn.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.server.protocol.ProtocolParser;
import br.imd.ufrn.server.protocol.action.*;

public class HttpServer implements Server {

  @Override
  public void run(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
         ServerSocket serverSocket = new ServerSocket(port, 1000)) {
      System.out.println("server started at port " + port);
      sendRegisterHttp(port);

      while (true) {
        Socket conn = serverSocket.accept();
        executor.submit(() -> handleHttpRequest(conn));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void handleHttpRequest(Socket conn) {
    try (
            conn;
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))
    ) {
      String requestLine = in.readLine();
      if (requestLine == null) return;

      String[] requestParts = requestLine.split(" ");
      if (requestParts.length < 2) {
        sendResponse(out, 400, "Bad Request", "Invalid request line");
        return;
      }

      String method = requestParts[0];
      String path = requestParts[1];

      int contentLength = 0;
      String line;
      while (!(line = in.readLine()).isEmpty()) {
        if (line.toLowerCase().startsWith("content-length:")) {
          contentLength = Integer.parseInt(line.split(":")[1].trim());
        }
      }

      String body = null;
      if (contentLength > 0) {
        char[] bodyChars = new char[contentLength];
        in.read(bodyChars);
        body = new String(bodyChars).trim();
      }

      if (method.equals("GET") && path.startsWith("/doc/")) {
        String docId = path.substring("/doc/".length());
        Action action = new Get(docId);
        String result = handleAction(action);
        sendResponse(out, 200, "OK", result);
      } else if (method.equals("PUT") && path.startsWith("/doc/")) {
        if (body == null) {
          sendResponse(out, 400, "Bad Request", "Missing body");
          return;
        }
        String docId = path.substring("/doc/".length());
        Action action = new Edit(docId, body);
        String result = handleAction(action);
        sendResponse(out, 200, "OK", result);
      } else if (method.equals("CREATE") && path.startsWith("/doc/")) {
        String docId = path.substring("/doc/".length());
        Action action = new Create(docId);
        String result = handleAction(action);
        sendResponse(out, 201, "Created", result);
      } else {
        sendResponse(out, 404, "Not Found", "Unknown endpoint");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String handleAction(Action action) {
    return switch (action) {
      case Create create -> "Document " + create.documentName() + " created";
      case Edit edit -> "Edited " + edit.documentName() + " to " + edit.content();
      case Get get -> "Document " + get.documentName() + " content is: ...";
    };
  }

  private void sendResponse(BufferedWriter out, int status, String statusText, String body) throws IOException {
    out.write("HTTP/1.0 " + status + " " + statusText + "\r\n");
    out.write("Content-Type: text/plain\r\n");
    out.write("Content-Length: " + body.length() + "\r\n");
    out.write("\r\n");
    out.write(body);
    out.flush();
  }

  private void sendRegisterHttp(int port) {
    try (
            Socket socket = new Socket("localhost", 8081);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
    ) {
      String body = String.valueOf(port);
      out.write("POST /register HTTP/1.0\r\n");
      out.write("Content-Type: text/plain\r\n");
      out.write("Content-Length: " + body.length() + "\r\n");
      out.write("\r\n");
      out.write(body);
      out.flush();
    } catch (Exception e) {
      throw new RuntimeException("Error when sending HTTP register", e);
    }
  }
}
