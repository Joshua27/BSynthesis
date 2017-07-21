package de.hhu.stups.bsynthesis.services;

public enum SolverBackend {
  PROB, Z3;

  @Override
  public String toString() {
    if (Z3.equals(this)) {
      return "z3";
    } else {
      return "proB";
    }
  }
}
