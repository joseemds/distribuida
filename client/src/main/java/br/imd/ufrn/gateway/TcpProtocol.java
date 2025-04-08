package br.imd.ufrn.gateway;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class TcpProtocol implements ProtocolHandler<Socket> {
    private final int port;
    private final ExecutorService executor;

    TcpProtocol(int port, ExecutorService executor) {
        this.port = port;
        this.executor = executor;
    }

    public void startServer(){
        try(ServerSocket serverSocket = new ServerSocket(this.port, 300)) {
            while (true){
                Socket conn = serverSocket.accept();
                executor.submit(() -> this.handleRequest(conn));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRequest(Socket conn, int nextServer){

    }
}
