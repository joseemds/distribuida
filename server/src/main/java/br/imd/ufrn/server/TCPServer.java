package br.imd.ufrn.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.server.protocol.ProtocolParser;
import br.imd.ufrn.server.protocol.action.*;
import br.imd.ufrn.server.protocol.action.Action;
import br.imd.ufrn.server.versionvector.Ordering;
import br.imd.ufrn.server.versionvector.VersionVector;
import br.imd.ufrn.server.versionvector.VersionedNote;

public class TCPServer implements Server {

  private final String serverId = "J";
  private final ProtocolParser parser = new ProtocolParser();
  private final Map<String, VersionedNote> documents = new ConcurrentHashMap<>();


  @Override
  public void run(int port) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        ServerSocket serverSocket = new ServerSocket(port, 1000); ) {
      System.out.println("server started at port " + port);
      sendRegister(port);
      while (true) {
        Socket conn = serverSocket.accept();

        executor.submit(() -> handleRequest(conn));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void handleRequest(Socket conn) {
    try (conn;
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {

      String message = in.readLine();
      System.out.println("Received: " + message);

      Action action = parser.parse(message);

      String response =
          switch (action) {
            case Create create -> handleCreate(create);
            case Edit edit -> handleEdit(edit);
            case Get get -> handleGet(get);
          };

      out.println(response);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendRegister(int port) {
    try (Socket socket = new Socket("localhost", 8081);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

      String message = "register:" + port;
      out.println(message);

    } catch (Exception e) {
      throw new RuntimeException("Error when sending register", e);
    }
  }


  private String handleCreate(Create create){
    String documentName = create.documentName();

    if (documents.containsKey(documentName)) {
      return "ERROR: Document " + documentName + " already exists";
    }
    VersionVector initialVector = new VersionVector();
    initialVector = initialVector.increment(serverId);

    VersionedNote document = new VersionedNote("", initialVector);
    documents.put(documentName, document);

    return "SUCCESS: Document " + documentName + " created";


  };
  private String handleEdit(Edit edit) {
    String documentName = edit.documentName();
    String newContent = edit.content();

    VersionedNote serverNote = documents.get(documentName);
    if (serverNote == null) {
      return "ERROR: Document " + documentName + " does not exist";
    }

    VersionVector updatedVector = serverNote.getVersionVector().increment(serverId);
    Ordering comparison = VersionVector.compare(serverNote.getVersionVector(), serverNote.getVersionVector());

    String s = switch (comparison) {
      case Before -> "before";
      case After -> "after";
      case Concurrent -> "concurrent";
    };

    VersionedNote updatedDocument = new VersionedNote(newContent, updatedVector);

    documents.put(documentName, updatedDocument);

    System.out.println("Edited document " + documentName + " with new version " + updatedVector);
    return "SUCCESS: Document " + documentName + " edited | " + updatedDocument.toString();
  }
  private String handleGet(Get get) {
    String documentName = get.documentName();

    VersionedNote document = documents.get(documentName);
    if (document == null) {
      return "ERROR: Document " + documentName + " does not exist";
    }

    return "SUCCESS: " + document.toString();
  }}
