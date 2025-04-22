package br.imd.ufrn.server;

import br.imd.ufrn.server.protocol.ProtocolParser;
import br.imd.ufrn.server.protocol.action.*;
import br.imd.ufrn.server.versionvector.VersionVector;
import br.imd.ufrn.server.versionvector.VersionedDocument;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractServer implements Server {
    protected final String serverId = "id";
    protected final ProtocolParser parser = new ProtocolParser();
    protected final Map<String, VersionedDocument> documents = new ConcurrentHashMap<>();

    @Override
    public abstract void run(int port);

    protected abstract void writeLog();

    protected abstract void sendRegister(int port);

    protected abstract void propagateChanges(VersionedDocument versionedDoc);

    protected String handleAction(Action action) {
        return switch (action) {
            case Edit edit -> handleEdit(edit);
            case Get get -> handleGet(get);
        };
    }

    protected String handleEdit(Edit edit) {
        String documentName = edit.documentName();
        String content = edit.content();

        VersionedDocument versionedDoc = documents.getOrDefault(documentName, null);

        if (versionedDoc != null) {
            String oldContent = versionedDoc.getContent();
            versionedDoc.setContent(oldContent + "\n" + content);
            versionedDoc.getVersionVector().increment(serverId);
        } else {
            versionedDoc = new VersionedDocument(content, new VersionVector());
            documents.put(documentName, versionedDoc);
        }

        propagateChanges(versionedDoc);

        return "SUCCESS: Document " + documentName + " edited to: " + versionedDoc.getContent();
    }

    protected String handleGet(Get get) {
        String documentName = get.documentName();

        if (!documents.containsKey(documentName)) {
            return "ERROR: Document " + documentName + " does not exist";
        }

        return "SUCCESS: Document " + documentName + " content is: " + documents.get(documentName);
    }

}