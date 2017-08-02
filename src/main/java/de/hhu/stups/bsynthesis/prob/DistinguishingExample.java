package de.hhu.stups.bsynthesis.prob;

import com.google.common.base.Joiner;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A distinguishing example containing input and output states represented as lists of {@link
 * VarValueTuple}.
 */
public final class DistinguishingExample {

  private final List<VarValueTuple> inputTuples;
  private final List<VarValueTuple> outputTuples;

  DistinguishingExample(final List<VarValueTuple> inputTuples,
                        final List<VarValueTuple> outputTuples) {
    this.inputTuples = inputTuples;
    this.outputTuples = outputTuples;
  }

  public List<VarValueTuple> getInputTuples() {
    return inputTuples;
  }

  public List<VarValueTuple> getOutputTuples() {
    return outputTuples;
  }

  public String getInputStateEquality() {
    return getStateEquality(inputTuples);
  }

  public String getOutputStateEquality() {
    return getStateEquality(outputTuples);
  }

  /**
   * Return a string of equalities between variable and value.
   */
  private String getStateEquality(final List<VarValueTuple> tuples) {
    return Joiner.on(" & ").join(tuples.stream().map(varValueTuple ->
        varValueTuple.getVar() + "=" + varValueTuple.getValue().replace("'", ""))
        .collect(Collectors.toList()));
  }
}
