package br.imd.ufrn.log.protocol;

import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public abstract class AbstractServer {
  private final Map<String, Stack<String>> logs = new ConcurrentHashMap<>();

  public abstract void run(int port, ExecutorService executor);

  public void log(String id, String message) {
    this.logs.computeIfAbsent(id, k -> new Stack<>()).push(message);
  }
}
