package br.imd.ufrn.gateway;

public class RunGateway {
  public static void main(String[] args) {
    int port = 8080;
    String protocol = args[0].toUpperCase();
    ApiGateway gateway = new ApiGateway(port, protocol);
    System.out.println("Starting gateway at port " + port + "with protocol " + protocol);
    gateway.start();
  }
}
