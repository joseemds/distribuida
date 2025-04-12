package br.imd.ufrn.gateway;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiGateway {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private int currentServerIndex = 0;
    private final ProtocolHandler protocolHandler;
    private final List<Integer> registeredServers = new ArrayList<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ApiGateway(int port, String protocolType) {
        this.port = port;
        this.protocolHandler = createProtocolHandler(protocolType);
    }

    public void start() {
        executor.submit(() -> protocolHandler.handleServerRegister(this::registerServer));
        this.protocolHandler.startServer();
    }

    private void registerServer(Integer port) {
        this.registeredServers.add(port);
    }

    private int getNextServer() {
        if (registeredServers.isEmpty()) {
            throw new IllegalStateException("No servers registered");
        }

        int serverPort = registeredServers.get(currentServerIndex);
        currentServerIndex = (currentServerIndex + 1) % registeredServers.size();
        return serverPort;
    }

    private ProtocolHandler createProtocolHandler(String protocol){
        return switch (protocol) {
            case "TCP" -> new TcpProtocol(this.port, this.executor, this::getNextServer);
            case "UDP" -> throw new Error("Unimplemented");
            case "HTTP" -> throw new Error("Unimplemented");
            default -> throw new Error("Unsupported protocol");
        };
    }
}
