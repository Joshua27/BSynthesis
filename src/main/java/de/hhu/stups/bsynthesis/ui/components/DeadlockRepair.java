package de.hhu.stups.bsynthesis.ui.components;

public enum DeadlockRepair {
  REMOVE_DEADLOCK, RESOLVE_DEADLOCK;

  public boolean isRemoveDeadlock() {
    return REMOVE_DEADLOCK.equals(this);
  }
}
