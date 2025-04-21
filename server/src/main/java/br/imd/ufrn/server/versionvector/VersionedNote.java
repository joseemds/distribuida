package br.imd.ufrn.server.versionvector;

import java.util.Map;
import java.util.TreeMap;

public class VersionedNote {
  private String content;
  private VersionVector versionVector;

  public VersionedNote(String content, VersionVector versionVector) {
    this.content = content;
    this.versionVector = versionVector;
  }

  public String getContent() {
    return content;
  }

  public VersionVector getVersionVector() {
    return versionVector;

  }

  public VersionedNote merge(VersionedNote other, String serverId) {
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

    return new VersionedNote(mergedContent, newVector);
  }
}
