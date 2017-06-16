package de.hhu.stups.bsynthesis.prob;

import de.prob.prolog.output.IPrologTermOutput;

class InputOutputExample {

  private final ExampleState input;
  private final ExampleState output;

  InputOutputExample(final ExampleState input,
                     final ExampleState output) {
    this.input = input;
    this.output = output;
  }

  private void printInputStateToPrologTerm(final IPrologTermOutput prologTerm) {
    printState(input, prologTerm);
  }

  private void printOutputStateToPrologTerm(final IPrologTermOutput prologTerm) {
    printState(output, prologTerm);
  }

  private void printState(final ExampleState exampleState,
                          final IPrologTermOutput prologTerm) {
    if (exampleState != null) {
      exampleState.printStateToPrologTerm(prologTerm);
      return;
    }
    // empty list if null
    prologTerm.openList().closeList();
  }

  void printInputOutputStateToPrologTerm(final IPrologTermOutput pto) {
    printInputStateToPrologTerm(pto);
    printOutputStateToPrologTerm(pto);
  }

  @Override
  public String toString() {
    return input.toString() + " " + output.toString();
  }
}
