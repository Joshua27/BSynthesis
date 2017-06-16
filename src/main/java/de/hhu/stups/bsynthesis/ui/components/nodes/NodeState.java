package de.hhu.stups.bsynthesis.ui.components.nodes;

public enum NodeState {
  VALID, INVARIANT_VIOLATED, TENTATIVE;

  public boolean isValid() {
    return this.equals(VALID);
  }

  public boolean isInvariantViolated() {
    return this.equals(INVARIANT_VIOLATED);
  }

  public boolean isTentative() {
    return this.equals(TENTATIVE);
  }

}
