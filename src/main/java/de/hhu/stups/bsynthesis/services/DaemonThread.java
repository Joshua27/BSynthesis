package de.hhu.stups.bsynthesis.services;

public interface DaemonThread {
  /**
   * Create a thread that is set to be a daemon.
   */
  static Thread getDaemonThread(final Runnable runnable) {
    final Thread thread = new Thread(runnable);
    thread.setDaemon(true);
    return thread;
  }
}
