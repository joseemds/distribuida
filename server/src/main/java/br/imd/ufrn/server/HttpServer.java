package br.imd.ufrn.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.server.protocol.action.*;
import br.imd.ufrn.server.versionvector.VersionedDocument;

public class HttpServer extends AbstractServer {

  @Override
  public void run(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        ServerSocket serverSocket = new ServerSocket(port, 1000)) {
      System.out.println("HTTP server started at port " + port);
      sendRegister(port);

      while (true) {
        Socket conn = serverSocket.accept();
        executor.submit(() -> handleHttpRequest(conn));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void writeLog(String target, String message) {
    try (Socket socket = new Socket("localhost", 9999);
         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

      String body = target + ":" + message;
      out.write("POST /log HTTP/1.0\r\n");
      out.write("Content-Type: text/plain\r\n");
      out.write("Content-Length: " + body.length() + "\r\n");
      out.write("\r\n");
      out.write(body);
      out.flush();

    } catch (Exception e) {
    }
  }


  @Override
  protected void sendRegister(int port) {
    try (Socket socket = new Socket("localhost", 8081);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
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

  private void handleHttpRequest(Socket conn) {
    try (conn;
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
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
      while ((line = in.readLine()) != null && !line.isEmpty()) {
        if (line.toLowerCase().startsWith("content-length:")) {
          contentLength = Integer.parseInt(line.split(":")[1].trim());
        }
      }

      String body = null;
      if (contentLength > 0) {
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;
        while (totalRead < contentLength) {
          int read = in.read(bodyChars, totalRead, contentLength - totalRead);
          if (read == -1) {
            throw new IOException("Unexpected end of stream while reading body");
          }
          totalRead += read;
        }
        body = new String(bodyChars).trim();
      }

      if (method.equals("GET") && path.equals("/healthcheck")) {
        System.out.println("Healthcheck received, sending healthy response.");
        sendResponse(out, 200, "OK", "healthy");
      } else if (method.equals("GET") && path.startsWith("/doc/")) {
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
      } else {
        sendResponse(out, 404, "Not Found", "Unknown endpoint");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendResponse(BufferedWriter out, int status, String statusText, String body)
      throws IOException {
    out.write("HTTP/1.0 " + status + " " + statusText + "\r\n");
    out.write("Content-Type: text/plain\r\n");
    out.write("Content-Length: " + body.length() + "\r\n");
    out.write("\r\n");
    out.write(body);
    out.flush();
  }

  protected void propagateChanges(VersionedDocument versionedDoc) {
    System.out.println("Propagating changes for document: " + versionedDoc.getContent());
  }
}
