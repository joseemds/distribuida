package br.imd.ufrn.server.versionvector;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

public class VersionedDocument {
  private String content;
  private VersionVector versionVector;
  private Instant timestamp;

  public VersionedDocument(String content, VersionVector versionVector) {
    this.content = content;
    this.versionVector = versionVector;
    this.timestamp = Instant.now();
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public VersionVector getVersionVector() {
    return versionVector;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public VersionedDocument merge(VersionedDocument other, String serverId) {
    String mergedContent = this.content + "\n[MERGED WITH: " + other.content + "]";

    TreeMap<String, Long> mergedVersions = new TreeMap<>();

    for (Map.Entry<String, Long> entry : this.versionVector.getVersions().entrySet()) {
      mergedVersions.put(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, Long> entry : other.versionVector.getVersions().entrySet()) {
      String key = entry.getKey();
      Long otherValue = entry.getValue();
      Long thisValue = mergedVersions.getOrDefault(key, 0L);
      mergedVersions.put(key, Math.max(thisValue, otherValue));
    }

    VersionVector newVector = new VersionVector(mergedVersions);
    newVector = newVector.increment(serverId);

    return new VersionedDocument(mergedContent, newVector);
  }
}
