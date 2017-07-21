package de.hhu.stups.bsynthesis.prob;

import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.statespace.State;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ExamplesToProlog {

  /**
   * Print a set of {@link InputOutputExample} as a prolog list to a given
   * {@link IPrologTermOutput prolog term}.
   */
  static void printList(final IPrologTermOutput pto,
                        final Set<InputOutputExample> examples) {
    pto.openList();
    examples.forEach(example -> {
      pto.openTerm(",");
      example.printInputOutputStateToPrologTerm(pto);
      pto.closeTerm();
    });
    pto.closeList();
  }


  /**
   * Create a set of {@link InputOutputExample} for a given list of {@link BasicNode}.
   */
  static Set<InputOutputExample> getInputOutputExamples(final List<BasicNode> examples,
                                                        final Set<String> currentVarNames) {
    final Set<InputOutputExample> inputOutputExamples = new HashSet<>();
    examples.forEach(basicNode -> addInputOutputExample(inputOutputExamples,
        currentVarNames, basicNode));
    return inputOutputExamples;
  }

  /**
   * Create a {@link InputOutputExample} from the given {@link BasicNode} and add it to the given
   * set.
   */
  static void addInputOutputExample(final Set<InputOutputExample> inputOutputExamples,
                                    final Set<String> currentVarNames,
                                    final BasicNode basicNode) {
    if (basicNode instanceof StateNode) {
      // guard or invariant
      addStateNode(inputOutputExamples, currentVarNames, (StateNode) basicNode);
      return;
    }
    if (basicNode instanceof TransitionNode) {
      // operation / substitution
      final State inputState = ((TransitionNode) basicNode).getInputState();
      final State outputState = ((TransitionNode) basicNode).getOutputState();
      inputOutputExamples.add(
          new InputOutputExample(new ExampleState(inputState, currentVarNames),
              new ExampleState(outputState, currentVarNames)));
    }
  }

  /**
   * Add a {@link StateNode} to the given set of {@link InputOutputExample}.
   */
  static void addStateNode(final Set<InputOutputExample> inputOutputExamples,
                           final Set<String> currentVarNames,
                           final StateNode stateNode) {
    final State state = stateNode.getState();
    if (stateNode.isValidatedPositive()) {
      // set the positive state to be an input examples and compute the corresponding output state
      final StateNode successorState = stateNode.getSuccessorFromTrace();
      if (successorState != null) {
        inputOutputExamples.add(new InputOutputExample(
            new ExampleState(state, currentVarNames),
            new ExampleState(successorState.getState(), currentVarNames)));
      } else {
        // no successor, just add the same node since we synthesize guard/invariant and
        // split transitions
        inputOutputExamples.add(
            new InputOutputExample(new ExampleState(state, currentVarNames),
                new ExampleState(state, currentVarNames)));
      }
      return;
    }
    // otherwise, set the violating state to be an output and set its predecessor to be the input
    // if available (synthesizing a precondition/guard we need to exclude the violating state's
    // predecessor)
    inputOutputExamples.add(new InputOutputExample(
        new ExampleState(stateNode.getPredecessor(), currentVarNames),
        new ExampleState(state, currentVarNames)));
  }
}
