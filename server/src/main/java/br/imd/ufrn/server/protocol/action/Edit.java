package br.imd.ufrn.server.protocol.action;

public record Edit(String documentName, String content) implements Action {}
