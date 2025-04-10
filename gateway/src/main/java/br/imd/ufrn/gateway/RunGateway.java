package br.imd.ufrn.gateway;

public class RunGateway {
  public static void main(String[] args) {
    int port = 8080;
    String protocol = "TCP";
    ApiGateway gateway = new ApiGateway(port, "TCP");
    System.out.println("Starting gateway at port " + port + "with protocol " + protocol);
    gateway.start();
  }
}
