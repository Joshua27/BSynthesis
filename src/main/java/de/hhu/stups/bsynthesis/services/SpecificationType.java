package de.hhu.stups.bsynthesis.services;

public enum SpecificationType {
  CLASSICAL_B, EVENT_B;

  public boolean isClassicalB() {
    return CLASSICAL_B.equals(this);
  }
}
