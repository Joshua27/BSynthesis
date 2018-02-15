package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.BindingGenerator;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.prolog.term.PrologTerm;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.stream.Collectors;

/**
 * Collect valid transitions and negative input states for an operation.
 * The prolog predicate returns strings with state equality predicates that we can for example
 * use within {@link de.prob.animator.command.FindStateCommand}.
 */
public class VisualizeOperationCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME =
      "get_valid_and_invalid_equality_predicates_for_operation_";
  private static final String VALID_TRANSITIONS_EQS = "ValidPrettyEqualityTuples";
  private static final String INVALID_STATES_EQS = "InvalidPrettyEqualities";
  private static final String IGNORED_IDS = "IgnoredIDs";

  private final String operationName;
  private final int validTransitionsAmount;
  private final int invalidStatesAmount;

  private final ListProperty<VarValueTuple> validTransitionEqualitiesProperty;
  private final ListProperty<String> invalidStateEqualitiesProperty;
  private final ListProperty<String> ignoredIDsProperty;

  /**
   * Initialize properties and constructor parameters.
   */
  public VisualizeOperationCommand(final String operationName,
                                   final int validTransitionsAmount,
                                   final int invalidStatesAmount) {
    this.operationName = operationName;
    this.validTransitionsAmount = validTransitionsAmount;
    this.invalidStatesAmount = invalidStatesAmount;
    validTransitionEqualitiesProperty =
        new SimpleListProperty<>(FXCollections.observableArrayList());
    invalidStateEqualitiesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    ignoredIDsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME)
        .printAtom(operationName)
        .printNumber(validTransitionsAmount)
        .printNumber(invalidStatesAmount)
        .printVariable(VALID_TRANSITIONS_EQS)
        .printVariable(INVALID_STATES_EQS)
        .printVariable(IGNORED_IDS)
        .closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    validTransitionEqualitiesProperty.addAll(BindingGenerator.getList(
        bindings.get(VALID_TRANSITIONS_EQS)).stream()
        .map(this::getVarValueTupleFromProlog).collect(Collectors.toList()));
    invalidStateEqualitiesProperty.addAll(BindingGenerator.getList(
        bindings.get(INVALID_STATES_EQS)).stream()
        .map(prologTerm -> prologTerm.getFunctor().replace("'", ""))
        .collect(Collectors.toList()));
    ignoredIDsProperty.addAll(BindingGenerator.getList(
        bindings.get(IGNORED_IDS)).stream()
        .map(prologTerm -> prologTerm.getFunctor().replace("'", ""))
        .collect(Collectors.toList()));
  }

  private VarValueTuple getVarValueTupleFromProlog(final PrologTerm prologTerm) {
    final CompoundPrologTerm compoundPrologTerm = BindingGenerator.getCompoundTerm(prologTerm, 2);
    return new VarValueTuple(compoundPrologTerm.getArgument(1).toString().replace("'", ""),
        compoundPrologTerm.getArgument(2).toString().replace("'", ""));
  }

  public ListProperty<VarValueTuple> validTransitionEqualitiesProperty() {
    return validTransitionEqualitiesProperty;
  }

  public ListProperty<String> invalidStateEqualitiesProperty() {
    return invalidStateEqualitiesProperty;
  }

  public ListProperty<String> ignoredIDsProperty() {
    return ignoredIDsProperty;
  }
}