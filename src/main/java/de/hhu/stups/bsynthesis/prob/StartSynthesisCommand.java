package de.hhu.stups.bsynthesis.prob;

import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.prob.animator.command.AbstractCommand;
import de.prob.parser.BindingGenerator;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.prolog.term.PrologTerm;
import de.prob.statespace.State;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StartSynthesisCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "start_synthesis";
  private static final String DISTINGUISHING_EXAMPLE = "Distinguishing";
  private static final String MODIFIED_MACHINE = "NewMachine";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String currentOperation;
  private final SynthesisType synthesisType;
  private final Set<InputOutputExample> positiveExamples;
  private final Set<InputOutputExample> negativeExamples;
  private final BLibrary selectedLibraryComponents;
  private final BooleanProperty synthesisSucceededProperty;
  private final StringProperty modifiedMachineCodeProperty;
  private final ObjectProperty<DistinguishingExample> distinguishingExampleProperty;

  /**
   * Start the synthesis workflow by calling the prolog backend.
   */
  public StartSynthesisCommand(final BLibrary selectedLibraryComponents,
                               final String currentOperation,
                               final SynthesisType synthesisType,
                               final List<BasicNode> positiveExamples,
                               final List<BasicNode> negativeExamples) {
    this.currentOperation = currentOperation;
    this.synthesisType = synthesisType;
    this.positiveExamples = getInputOutputExamples(positiveExamples);
    this.negativeExamples = getInputOutputExamples(negativeExamples);
    this.selectedLibraryComponents = selectedLibraryComponents;
    synthesisSucceededProperty = new SimpleBooleanProperty(false);
    modifiedMachineCodeProperty = new SimpleStringProperty("");
    distinguishingExampleProperty = new SimpleObjectProperty<>();
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME)
        .printAtom(selectedLibraryComponents.considerIfStatementsProperty().get()
            ? "yes" : "no")
        .printAtom(currentOperation)
        .printAtom(synthesisType.toString().toLowerCase());
    printPrologListFromExamples(pto, positiveExamples);
    printPrologListFromExamples(pto, negativeExamples);
    pto.printVariable(MODIFIED_MACHINE).printVariable(DISTINGUISHING_EXAMPLE).closeTerm();
    System.out.println(pto.toString());
    logger.info("Start synthesis prolog backend by calling prob2_interface: {}", pto);
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
    final String newMachineCode = bindings.get(MODIFIED_MACHINE).getFunctor();
    if ("none".equals(newMachineCode)) {
      logger.info("Distinguishing example: {}", bindings.get(DISTINGUISHING_EXAMPLE));
      setDistinguishingExample(bindings.get(DISTINGUISHING_EXAMPLE));
      return;
    }
    synthesisSucceededProperty.set(true);
    modifiedMachineCodeProperty.set(newMachineCode);
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
      addStateNode(inputOutputExamples, (StateNode) basicNode);
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

  private void addStateNode(final Set<InputOutputExample> inputOutputExamples,
                            final StateNode stateNode) {
    final State state = stateNode.getState();
    if (stateNode.isValidatedPositive()) {
      // set the positive state to be an input examples and compute the corresponding output state
      final StateNode successorState = stateNode.getSuccessorFromTrace();
      if (successorState != null) {
        inputOutputExamples.add(new InputOutputExample(
            new ExampleState(state),
            new ExampleState(successorState.getState())));
      } else {
        // no successor, just add the same node since we synthesize guard/invariant and
        // split transitions
        inputOutputExamples.add(
            new InputOutputExample(new ExampleState(state), new ExampleState(state)));
      }
      return;
    }
    // otherwise, set the violating state to be an output and set its predecessor to be the input
    // if available (synthesizing a precondition/guard we need to exclude the violating state's
    // predecessor)
    inputOutputExamples.add(new InputOutputExample(
        new ExampleState(stateNode.getPredecessor()),
        new ExampleState(state)));
  }

  private BasicNode setDistinguishingExample(final PrologTerm prologTerm) {
    final String resultFunctor = prologTerm.getFunctor();
    switch (resultFunctor) {
      case "state":
        final List<VarValueTuple> stateList = BindingGenerator.getList(prologTerm.getArgument(1))
            .stream().map(this::getVarValueTupleFromProlog).collect(Collectors.toList());
        distinguishingExampleProperty.set(
            new DistinguishingExample(stateList, Collections.emptyList()));
        break;
      case "transition":
        final List<VarValueTuple> inputStateList =
            BindingGenerator.getList(prologTerm.getArgument(1))
                .stream().map(this::getVarValueTupleFromProlog).collect(Collectors.toList());
        final List<VarValueTuple> outputStateList =
            BindingGenerator.getList(prologTerm.getArgument(2))
                .stream().map(this::getVarValueTupleFromProlog).collect(Collectors.toList());
        distinguishingExampleProperty.set(
            new DistinguishingExample(inputStateList, outputStateList));
        break;
      default:
        throw new AssertionError("Unexpected result of synthesis command.");
    }
    return null;
  }

  private VarValueTuple getVarValueTupleFromProlog(final PrologTerm prologTerm) {
    final CompoundPrologTerm compoundPrologTerm = BindingGenerator.getCompoundTerm(prologTerm, 2);
    return new VarValueTuple(compoundPrologTerm.getArgument(1).toString(),
        compoundPrologTerm.getArgument(2).toString());
  }

  public ObjectProperty<DistinguishingExample> distinguishingExampleProperty() {
    return distinguishingExampleProperty;
  }

  public BooleanProperty synthesisSucceededProperty() {
    return synthesisSucceededProperty;
  }

  public StringProperty modifiedMachineCodeProperty() {
    return modifiedMachineCodeProperty;
  }
}
