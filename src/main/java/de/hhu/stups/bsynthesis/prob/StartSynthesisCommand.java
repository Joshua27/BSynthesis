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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StartSynthesisCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "start_synthesis_from_ui";
  private static final String DISTINGUISHING_EXAMPLE = "Distinguishing";
  private static final String MODIFIED_MACHINE = "NewMachine";
  private static final String PROLOG_COMMAND_NAME2 = "start_synthesis_single_operation_from_ui";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String currentOperation;
  private final SynthesisType synthesisType;
  private final Set<InputOutputExample> positiveExamples;
  private final BooleanProperty synthesisSucceededProperty =
      new SimpleBooleanProperty(false);
  private final StringProperty modifiedMachineCodeProperty =
      new SimpleStringProperty();
  private final ObjectProperty<CompoundPrologTerm> synthesizedOperationProperty =
      new SimpleObjectProperty<>();
  private final StringProperty behaviorSatisfiedProperty =
      new SimpleStringProperty();
  private final ObjectProperty<DistinguishingExample> distinguishingExampleProperty =
      new SimpleObjectProperty<>();
  private final Set<InputOutputExample> negativeExamples;
  private final BLibrary selectedLibraryComponents;
  private final SolverBackend solverBackend;
  private final Set<String> currentVarNames;
  private final Set<CompoundPrologTerm> synthesizedOperations;
  private final boolean isImplicitIf;

  /**
   * Start the synthesis workflow by calling the prolog backend.
   */
  public StartSynthesisCommand(final BLibrary selectedLibraryComponents,
                               final String currentOperation,
                               final Set<String> currentVarNames,
                               final Set<CompoundPrologTerm> synthesizedOperations,
                               final SynthesisType synthesisType,
                               final Map<String, List<BasicNode>> examples,
                               final SolverBackend solverBackend) {
    this.currentOperation = currentOperation;
    this.synthesisType = synthesisType;
    this.positiveExamples = getInputOutputExamples(examples.get("valid"), currentVarNames);
    this.negativeExamples = getInputOutputExamples(examples.get("invalid"), currentVarNames);
    this.selectedLibraryComponents = selectedLibraryComponents;
    this.solverBackend = solverBackend;
    this.currentVarNames = currentVarNames;
    this.synthesizedOperations = synthesizedOperations;
    isImplicitIf = selectedLibraryComponents.considerIfStatementsProperty().get().isImplicit();
  }

  /**
   * Copy constructor for {@link StartSynthesisCommand} with a deep copy of {@link BLibrary}.
   */
  public StartSynthesisCommand(final StartSynthesisCommand startSynthesisCommand) {
    currentOperation = startSynthesisCommand.getCurrentOperation();
    synthesisType = startSynthesisCommand.getSynthesisType();
    positiveExamples = new HashSet<>(startSynthesisCommand.getPositiveExamples());
    negativeExamples = new HashSet<>(startSynthesisCommand.getNegativeExamples());
    selectedLibraryComponents = new BLibrary(startSynthesisCommand.getSelectedLibraryComponents());
    selectedLibraryComponents.solverTimeOutProperty()
        .bind(startSynthesisCommand.getSelectedLibraryComponents().solverTimeOutProperty());
    solverBackend = startSynthesisCommand.getSolverBackend();
    currentVarNames = startSynthesisCommand.getCurrentVarNames();
    synthesizedOperations = new HashSet<>();
    isImplicitIf = selectedLibraryComponents.considerIfStatementsProperty().get().isImplicit();
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    if (selectedLibraryComponents.considerIfStatementsProperty().get().isImplicit()
        && synthesisType.isAction()) {
      pto.openTerm(PROLOG_COMMAND_NAME2).openList();
      synthesizedOperations.forEach(pto::printTerm);
      pto.closeList();
    } else {
      pto.openTerm(PROLOG_COMMAND_NAME);
    }
    pto.printNumber(selectedLibraryComponents.getSolverTimeOut());
    selectedLibraryComponents.printToPrologTerm(pto);
    pto.printAtom(selectedLibraryComponents.doNotUseConstantsProperty().get()
        ? "yes" : "no")
        .printAtom(solverBackend.toString());
    printLibrary(pto);
    pto.printAtom(currentOperation)
        .printAtom(synthesisType.toEventBString().toLowerCase());
    printList(pto, positiveExamples);
    printList(pto, negativeExamples);
    pto.printVariable(MODIFIED_MACHINE).printVariable(DISTINGUISHING_EXAMPLE).closeTerm();
    logger.info("Start synthesis prolog backend by calling prob2_interface: {}", pto);
  }

  private void printLibrary(final IPrologTermOutput pto) {
    if (selectedLibraryComponents.considerIfStatementsProperty().get().isExplicit()) {
      // consider the current var names for if-statements, here it would be possible to let the user
      // additionally restrict the variables that if statements should be considered for, maybe
      // later
      pto.openList();
      currentVarNames.forEach(pto::printAtom);
      pto.closeList();
    } else if (!selectedLibraryComponents.considerIfStatementsProperty().get().isImplicit()) {
      // do not consider if-statements
      pto.openList().closeList();
    }
    // do not print anything for implicit if
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    final String newMachineCode = bindings.get(MODIFIED_MACHINE).getFunctor();
    switch (newMachineCode) {
      case "none":
        // distinguishing example
        logger.info("Distinguishing example: {}", bindings.get(DISTINGUISHING_EXAMPLE));
        setDistinguishingExample(bindings.get(DISTINGUISHING_EXAMPLE));
        modifiedMachineCodeProperty.set("none");
        break;
      case "operation_satisfied":
        // synthesizing an operation: an operation executes the provided behavior, and thus,
        // there is nothing to do for synthesis
        final String operationName = bindings.get(MODIFIED_MACHINE).getArgument(1).getFunctor();
        modifiedMachineCodeProperty.set("none");
        behaviorSatisfiedProperty.set(operationName);
        logger.info("Operation {} already satisfies the desired behavior.", operationName);
        synthesisSucceededProperty.set(true);
        break;
      default:
        // synthesis succeeded and the machine code has been adapted respectively
        if (selectedLibraryComponents.considerIfStatementsProperty().get().isImplicit()) {
          synthesisSucceededProperty.set(true);
          modifiedMachineCodeProperty.set("none");
          synthesizedOperationProperty.set(
              BindingGenerator.getCompoundTerm(bindings.get(MODIFIED_MACHINE), 2));
          break;
        }
        modifiedMachineCodeProperty.set(newMachineCode);
        synthesisSucceededProperty.set(true);
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

  public int getLibraryExpansion() {
    return selectedLibraryComponents.getLibraryExpansion();
  }

  public void setLibraryExpansion(final int libraryExpansion) {
    selectedLibraryComponents.setLibraryExpansion(libraryExpansion);
  }

  public boolean isDefaultLibraryConfiguration() {
    return selectedLibraryComponents.useDefaultLibraryProperty().get();
  }

  public SynthesisType getSynthesisType() {
    return synthesisType;
  }

  public BooleanProperty synthesisSucceededProperty() {
    return synthesisSucceededProperty;
  }

  public StringProperty modifiedMachineCodeProperty() {
    return modifiedMachineCodeProperty;
  }

  ObjectProperty<CompoundPrologTerm> synthesizedOperationProperty() {
    return synthesizedOperationProperty;
  }

  public StringProperty behaviorSatisfiedProperty() {
    return behaviorSatisfiedProperty;
  }

  public boolean expandLibrary() {
    return selectedLibraryComponents.expandDefaultLibrary();
  }

  private Set<InputOutputExample> getPositiveExamples() {
    return positiveExamples;
  }

  private Set<InputOutputExample> getNegativeExamples() {
    return negativeExamples;
  }

  private String getCurrentOperation() {
    return currentOperation;
  }

  private BLibrary getSelectedLibraryComponents() {
    return selectedLibraryComponents;
  }

  private SolverBackend getSolverBackend() {
    return solverBackend;
  }

  private Set<String> getCurrentVarNames() {
    return currentVarNames;
  }

  public boolean isImplicitIf() {
    return isImplicitIf;
  }
}
