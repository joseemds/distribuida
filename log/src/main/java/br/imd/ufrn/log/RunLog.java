package br.imd.ufrn.log;

public class RunLog {
  public static void main(String[] args) {
    int port = 9999;
    String protocol = args[0].toUpperCase();
    LogServer log = new LogServer(port, protocol);
    log.start();
  }
}
