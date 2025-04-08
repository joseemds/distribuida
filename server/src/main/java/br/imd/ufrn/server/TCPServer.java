package br.imd.ufrn.server;

import br.imd.ufrn.server.protocol.action.Action;
import br.imd.ufrn.server.protocol.ProtocolParser;
import br.imd.ufrn.server.protocol.action.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements Server{
	private final ProtocolParser parser = new ProtocolParser();
	@Override
	public void run() {
		System.out.println("server started ");
			try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
				 ServerSocket serverSocket = new ServerSocket(8080, 1000);
			){
				while(true){
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

			String response = switch (action) {
				case Create create -> "Document " + create.documentName() + "created";
				case Edit edit -> "Edited " + edit.documentName() + " to " + edit.content();
				case Get get -> "Document " + get.documentName() + "content is: ";
			};

			out.println(response);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
