package br.imd.ufrn.server;

public class RunServer {
  public static void main(String[] args) {
    Server s = ServerFactory.getServer(args[0].toLowerCase());
    int port = Integer.parseInt(args[1]);

    //    String gatewayUrl = args[1];
    s.run(port);
  }
}
