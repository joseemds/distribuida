package br.imd.ufrn.gateway;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class TcpProtocol implements ProtocolHandler<Socket> {
    private final int port;
    private final ExecutorService executor;
    private final Supplier<Integer> getNextServer;
    TcpProtocol(int port, ExecutorService executor, Supplier<Integer> getNextServer) {
        this.port = port;
        this.executor = executor;
        this.getNextServer = getNextServer;
    }

    public void startServer(){
        try(ServerSocket serverSocket = new ServerSocket(this.port, 300)) {
            while (true){
                Socket conn = serverSocket.accept();
                executor.submit(() -> this.handleRequest(conn, this.getNextServer));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRequest(Socket clientSocket, Supplier<Integer> getNextServer){
        int nextPort = this.getNextServer.get();

        try{
            Socket serverSocket = new Socket("localhost", nextPort);
            InputStream clientInput = clientSocket.getInputStream();
            OutputStream serverOutput = serverSocket.getOutputStream();
            clientInput.transferTo(serverOutput);

            serverSocket.close();
            clientSocket.close();

        }   catch (Exception e) {

        }
    }

    @Override
    public void handleService(){

    }
}
