package br.imd.ufrn.server.protocol.action;

public sealed interface Action permits Create, Get, Edit {}
