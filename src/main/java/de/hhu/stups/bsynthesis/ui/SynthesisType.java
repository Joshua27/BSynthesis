package de.hhu.stups.bsynthesis.ui;

public enum SynthesisType {
  ACTION, GUARD_OR_INVARIANT, DEADLOCK;
  @Override
  public String toString() {
    switch(this) {
      case ACTION:
        return "Action";
      case GUARD_OR_INVARIANT:
        return "Guard or Invariant";
      case DEADLOCK:
        return "Deadlock";
      default:
        return "";
    }
  }

  public boolean isAction() {
    return this.equals(ACTION);
  }
}
