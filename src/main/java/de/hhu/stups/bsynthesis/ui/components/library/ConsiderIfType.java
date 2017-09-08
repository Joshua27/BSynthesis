package de.hhu.stups.bsynthesis.ui.components.library;

public enum ConsiderIfType {
  NONE, EXPLICIT, IMPLICIT;

  public boolean isExplicit() {
    return this.equals(EXPLICIT);
  }

  public boolean isImplicit() {
    return this.equals(IMPLICIT);
  }
}
