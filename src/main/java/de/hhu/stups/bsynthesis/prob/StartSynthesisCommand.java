package de.hhu.stups.bsynthesis.prob;

import static de.hhu.stups.bsynthesis.prob.ExamplesToProlog.getInputOutputExamples;
import static de.hhu.stups.bsynthesis.prob.ExamplesToProlog.printList;

import de.hhu.stups.bsynthesis.services.SolverBackend;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.prob.animator.command.AbstractCommand;
import de.prob.parser.BindingGenerator;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.prolog.term.PrologTerm;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
  private final SolverBackend solverBackend;

  /**
   * Start the synthesis workflow by calling the prolog backend.
   */
  public StartSynthesisCommand(final BLibrary selectedLibraryComponents,
                               final String currentOperation,
                               final Set<String> currentVarNames,
                               final SynthesisType synthesisType,
                               final Map<String, List<BasicNode>> examples,
                               final SolverBackend solverBackend) {
    this.currentOperation = currentOperation;
    this.synthesisType = synthesisType;
    this.positiveExamples = getInputOutputExamples(examples.get("valid"), currentVarNames);
    this.negativeExamples = getInputOutputExamples(examples.get("invalid"), currentVarNames);
    this.selectedLibraryComponents = selectedLibraryComponents;
    this.solverBackend = solverBackend;
    synthesisSucceededProperty = new SimpleBooleanProperty(false);
    modifiedMachineCodeProperty = new SimpleStringProperty("");
    distinguishingExampleProperty = new SimpleObjectProperty<>();
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME);
    selectedLibraryComponents.printToPrologTerm(pto);
    pto.printAtom(solverBackend.toString())
        .printAtom(selectedLibraryComponents.considerIfStatementsProperty().get()
            ? "yes" : "no")
        .printAtom(currentOperation)
        .printAtom(synthesisType.toEventBString().toLowerCase());
    printList(pto, positiveExamples);
    printList(pto, negativeExamples);
    pto.printVariable(MODIFIED_MACHINE).printVariable(DISTINGUISHING_EXAMPLE).closeTerm();
    logger.info("Start synthesis prolog backend by calling prob2_interface: {}", pto);
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    final String newMachineCode = bindings.get(MODIFIED_MACHINE).getFunctor();
    switch (newMachineCode) {
      case "none":
        // distinguishing example
        logger.info("Distinguishing example: {}", bindings.get(DISTINGUISHING_EXAMPLE));
        setDistinguishingExample(bindings.get(DISTINGUISHING_EXAMPLE));
        break;
      case "operation_satisfied":
        // synthesizing an operation: an operation executes the provided behavior, and thus,
        // there is nothing to do for synthesis
        final String operationName = bindings.get(MODIFIED_MACHINE).getArgument(1).getFunctor();
        logger.info("Operation {} already satisfies the desired behavior.", operationName);
        synthesisSucceededProperty.set(true);
        break;
      default:
        // synthesis succeeded and the machine code has been adapted respectively
        synthesisSucceededProperty.set(true);
        modifiedMachineCodeProperty.set(newMachineCode);
        break;
    }
  }

  private void setDistinguishingExample(final PrologTerm prologTerm) {
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
