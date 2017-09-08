package de.hhu.stups.bsynthesis.prob;

public final class VarValueTuple {

  private final String var;
  private final String value;

  VarValueTuple(final String var,
                final String value) {
    this.var = var;
    this.value = value;
  }

  public String getVar() {
    return var;
  }

  public String getValue() {
    return value;
  }
}