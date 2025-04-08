package br.imd.ufrn.server;

public class ServerFactory {
	public static Server getServer(String protocol){
        return switch (protocol) {
            case "tcp" -> new TCPServer();
            case "udp" -> throw new Error("UDP not implemented yet");
            case "http" -> throw new Error("HTTP not implemented yet");
            case "grpc" -> throw new Error("gRPC not implemented yet");
            default -> throw new IllegalArgumentException("Protocolo não suportado");
        };

	}
	
}
