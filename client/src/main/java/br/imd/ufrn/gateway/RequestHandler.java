package br.imd.ufrn.gateway;

import java.net.Socket;

public interface RequestHandler {
    void handleRequest(Socket connection);
}
