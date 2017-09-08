package de.hhu.stups.bsynthesis.services;

public enum UncoveredError {
  INVARIANT_VIOLATION, DEADLOCK;

  /**
   * Return the corresponding {@link UncoveredError} for a specific error message.
   */
  public static UncoveredError getUncoveredErrorFromMessage(final String errorMessage) {
    if ("Deadlock found.".equals(errorMessage)) {
      return DEADLOCK;
    } else {
      return INVARIANT_VIOLATION;
    }
  }

  public boolean isInvariantViolation() {
    return INVARIANT_VIOLATION.equals(this);
  }

  public boolean isDeadlock() {
    return DEADLOCK.equals(this);
  }
}