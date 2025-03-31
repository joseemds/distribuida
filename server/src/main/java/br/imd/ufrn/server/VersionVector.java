package br.imd.ufrn.server;

import java.util.TreeMap;

public class VersionVector {
  private TreeMap<String, Long> versions;

  public VersionVector() {
    this.versions = new TreeMap<>();
  }

  public VersionVector(TreeMap<String, Long> versions) {
    this.versions = versions;
  }

  public VersionVector increment(String nodeId) {
    TreeMap<String, Long> versions = new TreeMap<>();
    versions.putAll(this.versions);
    Long version = versions.get(nodeId);
    if (version == null) {
      version = 1L;
    } else {
      version = version + 1L;
    }
    versions.put(nodeId, version);
    return new VersionVector(versions);
  }
}
