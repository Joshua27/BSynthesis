package de.hhu.stups.bsynthesis.services;

public enum ApplicationEventType {
  OPEN_TAB, CLOSE_APP;

  public boolean isOpenTab() {
    return this.equals(OPEN_TAB);
  }

  public boolean isCloseApp() {
    return this.equals(CLOSE_APP);
  }
}
