package de.hhu.stups.bsynthesis.ui;

import java.io.File;

public enum ContextEvent {
  SAVE, SAVE_AS, NEW , LOAD , SYNTHESIS_SUCCEEDED, RESET_CONTEXT;
  private File file;

  public File getFile() {
    return file;
  }

  public void setFile(final File file) {
    this.file = file;
  }
}
