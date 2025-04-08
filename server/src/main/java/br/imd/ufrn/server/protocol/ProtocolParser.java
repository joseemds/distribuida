package br.imd.ufrn.server.protocol;

import br.imd.ufrn.server.protocol.action.*;

import java.util.StringTokenizer;

public class ProtocolParser {

    public Action parse(String message){
        String[] parts = message.split(":");

        String command = parts[1];
        StringTokenizer tokenizer = new StringTokenizer(command, "(),");

        if(!tokenizer.hasMoreTokens()) throw new IllegalArgumentException("Invalid command");

        String action = tokenizer.nextToken();

        return switch (action) {
            case "create" -> {
                String documentName = tokenizer.nextToken().trim();
                yield new Create(documentName);
            }
            case "edit" -> {
                String documentName = tokenizer.nextToken().trim();
                String content = tokenizer.nextToken().trim();
                yield new Edit(documentName, content);
            }
            case "get" ->  {
                String documentName = tokenizer.nextToken().trim();
                yield new Get(documentName);
            }
            default -> throw new IllegalArgumentException("Invalid action");
        };
    }
}
