package br.imd.ufrn.server.versionvector;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class VersionVector implements Serializable {
  private TreeMap<String, Long> versions;

  public VersionVector() {
    this.versions = new TreeMap<>();
  }

  public VersionVector(TreeMap<String, Long> versions) {
    this.versions = versions;
  }

  public TreeMap<String, Long> getVersions() {
    return this.versions;
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

  public static Ordering compare(VersionVector v1, VersionVector v2) {
    validateNotNull(v1, v2);
    SortedSet<String> v1Nodes = v1.getVersions().navigableKeySet();
    SortedSet<String> v2Nodes = v2.getVersions().navigableKeySet();
    SortedSet<String> commonNodes = getCommonNodes(v1Nodes, v2Nodes);
    boolean v1Bigger = v1Nodes.size() > commonNodes.size();
    boolean v2Bigger = v2Nodes.size() > commonNodes.size();
    // Compare versions for common nodes
    for (String nodeId : commonNodes) {
      if (v1Bigger && v2Bigger) {
        break; // No need to compare further
      }
      long v1Version = v1.getVersions().get(nodeId);
      long v2Version = v2.getVersions().get(nodeId);
      if (v1Version > v2Version) {
        v1Bigger = true;
      } else if (v1Version < v2Version) {
        v2Bigger = true;
      }
    }
    return determineOrdering(v1Bigger, v2Bigger);
  }

  private static Ordering determineOrdering(boolean v1Bigger, boolean v2Bigger) {
    if (!v1Bigger && !v2Bigger) {
      return Ordering.Before;
    } else if (v1Bigger && !v2Bigger) {
      return Ordering.After;
    } else if (!v1Bigger && v2Bigger) {
      return Ordering.Before;
    } else {
      return Ordering.Concurrent;
    }
  }

  private static void validateNotNull(VersionVector v1, VersionVector v2) {
    if (v1 == null || v2 == null) {
      throw new IllegalArgumentException("Cant compare null vectors");
    }
  }

  private static SortedSet<String> getCommonNodes(
      SortedSet<String> v1Nodes, SortedSet<String> v2Nodes) {
    SortedSet<String> commonNodes = new TreeSet<String>(v1Nodes);
    commonNodes.retainAll(v2Nodes);
    return commonNodes;
  }
}
