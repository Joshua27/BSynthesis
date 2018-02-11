package de.hhu.stups.bsynthesis.ui;

import java.io.File;

public class ContextEvent {

  private final ContextEventType contextEventType;
  private final File file;

  public ContextEvent(final ContextEventType contextEventType,
                      final File file) {
    this.contextEventType = contextEventType;
    this.file = file;
  }

  public ContextEventType getContextEventType() {
    return contextEventType;
  }

  public File getFile() {
    return file;
  }
}
