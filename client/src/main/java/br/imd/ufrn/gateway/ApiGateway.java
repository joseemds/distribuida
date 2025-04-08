package br.imd.ufrn.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiGateway {
    private static final int DEFAULT_PORT = 8080;
    private final int port;
    private final List<Integer> registeredServers = new ArrayList<>() ;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final int counter =0;
    private volatile boolean running = true;

    public ApiGateway(int port, String protocolType) {
        this.port = port;
    }

    public void start() {
    }

    public void stop() {

    }

    public void registerServer(String host, int port) {

    }

    private int getNextServer() {
        return -1;
    }

    public static void main(String[] args) {

    }
