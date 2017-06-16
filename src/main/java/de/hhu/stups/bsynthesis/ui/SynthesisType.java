package de.hhu.stups.bsynthesis.ui;

public enum SynthesisType {
  ACTION, GUARD, INVARIANT, DEADLOCK, NONE;

  @Override
  public String toString() {
    switch (this) {
      case ACTION:
        return "Action";
      case GUARD:
        return "Guard";
      case INVARIANT:
        return "Invariant";
      case DEADLOCK:
        return "Deadlock";
      case NONE:
        return "Undefined";
      default:
        return "";
    }
  }

  public boolean isAction() {
    return this.equals(ACTION);
  }

  public boolean isUndefined() {
    return this.equals(NONE);
  }
}
