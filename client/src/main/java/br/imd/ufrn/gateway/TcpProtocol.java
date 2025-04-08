package br.imd.ufrn.gateway;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TcpProtocol implements ProtocolHandler {
    private int port;
    private ExecutorService executor;
    private RequestHandler handler;

    TcpProtocol(int port, ExecutorService executor, RequestHandler handler) {
        this.port = port;
        this.executor = executor;
        this.handler = handler;
    }

    public void startServer(){
        try(ServerSocket serverSocket = new ServerSocket(8080, 300)) {
            while (true){
                Socket conn = serverSocket.accept();
                executor.submit(() -> handler.handleRequest(conn));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
