package de.hhu.stups.bsynthesis.services;

public enum VisualizationType {
  INVARIANT, OPERATION;

  public boolean isInvariant() {
    return this.equals(INVARIANT);
  }

}
