package de.hhu.stups.bsynthesis.prob;

import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import de.prob.statespace.State;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StartSynthesisCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "start_synthesis";
  private static final String SYNTHESIS_SUCCEEDED = "Succeeded";
  private static final String MODIFIED_MACHINE = "NewMachine";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String currentOperation;
  private final String synthesisType;
  private final Set<InputOutputExample> positiveExamples;
  private final Set<InputOutputExample> negativeExamples;
  private final BLibrary selectedLibraryComponents;
  private final BooleanProperty synthesisSucceededProperty;
  private final StringProperty modifiedMachineCodeProperty;

  /**
   * Start the synthesis workflow by calling the prolog backend.
   */
  public StartSynthesisCommand(final BLibrary selectedLibraryComponents,
                               final String currentOperation,
                               final SynthesisType synthesisType,
                               final List<BasicNode> positiveExamples,
                               final List<BasicNode> negativeExamples) {
    this.currentOperation = currentOperation;
    this.synthesisType = synthesisType.toString().toLowerCase();
    this.positiveExamples = getInputOutputExamples(positiveExamples);
    this.negativeExamples = getInputOutputExamples(negativeExamples);
    this.selectedLibraryComponents = selectedLibraryComponents;
    synthesisSucceededProperty = new SimpleBooleanProperty(false);
    modifiedMachineCodeProperty = new SimpleStringProperty();
  }

  private Set<InputOutputExample> getInputOutputExamples(final List<BasicNode> examples) {
    final Set<InputOutputExample> inputOutputExamples = new HashSet<>();
    examples.forEach(basicNode -> addInputOutputExample(inputOutputExamples, basicNode));
    return inputOutputExamples;
  }

  private void addInputOutputExample(final Set<InputOutputExample> inputOutputExamples,
                                     final BasicNode basicNode) {
    if (basicNode instanceof StateNode) {
      // guard or invariant
      final StateNode stateNode = ((StateNode) basicNode);
      final State state = stateNode.getState();
      if (stateNode.isValidatedPositive()) {
        // set the positive state to be an output examples and compute the corresponding input state
        final State predecessorState = stateNode.getPredecessor();
        if (predecessorState != null) {
          inputOutputExamples.add(
              new InputOutputExample(new ExampleState(predecessorState), new ExampleState(state)));
        }
        return;
      }
      // otherwise, set the violating state to be an output and set its predecessor to be the input
      // if available (synthesizing a precondition/guard we need to exclude the violating state's
      // predecessor)
      inputOutputExamples.add(new InputOutputExample(
          new ExampleState(stateNode.getPredecessor()),
          new ExampleState(state)));
      return;
    }
    if (basicNode instanceof TransitionNode) {
      // operation / substitution
      final State inputState = ((TransitionNode) basicNode).getInputState();
      final State outputState = ((TransitionNode) basicNode).getOutputState();
      inputOutputExamples.add(
          new InputOutputExample(new ExampleState(inputState), new ExampleState(outputState)));
    }
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME)
        .printAtom(selectedLibraryComponents.considerIfStatementsProperty().get()
            ? "yes" : "no")
        .printAtom(currentOperation)
        .printAtom(synthesisType);
    printPrologListFromExamples(pto, positiveExamples);
    printPrologListFromExamples(pto, negativeExamples);
    pto.printVariable(SYNTHESIS_SUCCEEDED).printVariable(MODIFIED_MACHINE).closeTerm();
    System.out.println(pto.toString());
    logger.info("Start synthesis prolog backend by calling prob2_interface:{}", pto);
  }

  private void printPrologListFromExamples(final IPrologTermOutput pto,
                                           final Set<InputOutputExample> examples) {
    pto.openList();
    examples.forEach(example -> {
      pto.openTerm(",");
      example.printInputOutputStateToPrologTerm(pto);
      pto.closeTerm();
    });
    pto.closeList();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    logger.info("Synthesis succeeded? {}", bindings.get(SYNTHESIS_SUCCEEDED));
    synthesisSucceededProperty.set("yes".equals(bindings.get(SYNTHESIS_SUCCEEDED).getFunctor()));
    modifiedMachineCodeProperty.set(bindings.get(MODIFIED_MACHINE).getFunctor());
  }

  public BooleanProperty synthesisSucceededProperty() {
    return synthesisSucceededProperty;
  }

  public StringProperty modifiedMachineCodeProperty() {
    return modifiedMachineCodeProperty;
  }
}
