package br.imd.ufrn.gateway;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiGateway {
    private static final int DEFAULT_PORT = 8080;
    private final int port;

    private final ProtocolHandler protocolHandler;
    private final List<Integer> registeredServers = new ArrayList<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ApiGateway(int port, String protocolType) {
        this.port = port;
        this.protocolHandler = createProtocolHandler(protocolType);
    }

    public void start() {
        this.protocolHandler.startServer();
    }

    public void registerServer(String host, int port) {

    }

    private int getNextServer() {
        return -1;
    }

    private ProtocolHandler createProtocolHandler(String protocol){
        return switch (protocol) {
            case "TCP" -> new TcpProtocol(this.port, this.executor);
            case "UDP" -> throw new Error("Unimplemented");
            case "HTTP" -> throw new Error("Unimplemented");
            default -> throw new Error("Unsupported protocol");
        };
    }
}
