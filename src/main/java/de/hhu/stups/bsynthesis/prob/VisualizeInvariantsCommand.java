package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.BindingGenerator;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Collect valid and invalid states for given invariants. The states are represented by a string
 * describing an equality predicate for each machine variable which can directly be used within
 * {@link de.prob.animator.command.FindStateCommand} to find the corresponding state id.
 */
public class VisualizeInvariantsCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME =
      "get_valid_and_invalid_equality_predicates_for_invariants";
  private static final String VALID_STATES_EQS = "ValidPrettyEqualities";
  private static final String INVALID_STATES_EQS = "InvalidPrettyEqualities";

  private final int validStatesAmount;
  private final int invalidStatesAmount;

  private final ListProperty<String> validStateEqualitiesProperty;
  private final ListProperty<String> invalidStateEqualitiesProperty;

  /**
   * Initialize properties and instance variables from constructor parameters.
   */
  public VisualizeInvariantsCommand(final int validStatesAmount,
                                    final int invalidStatesAmount) {
    this.validStatesAmount = validStatesAmount;
    this.invalidStatesAmount = invalidStatesAmount;
    validStateEqualitiesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    invalidStateEqualitiesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME)
        .printNumber(validStatesAmount)
        .printNumber(invalidStatesAmount)
        .printVariable(VALID_STATES_EQS)
        .printVariable(INVALID_STATES_EQS)
        .closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    final List<String> validStateEqualities =
        BindingGenerator.getList(bindings.get(VALID_STATES_EQS)).stream()
            .map(prologTerm -> prologTerm.getFunctor().replace("'", ""))
            .collect(Collectors.toList());
    validStateEqualitiesProperty.addAll(validStateEqualities);
    final List<String> invalidStateEqualities =
        BindingGenerator.getList(bindings.get(INVALID_STATES_EQS)).stream()
            .map(prologTerm -> prologTerm.getFunctor().replace("'", ""))
            .collect(Collectors.toList());
    invalidStateEqualitiesProperty.addAll(invalidStateEqualities);
  }

  public ListProperty<String> validStateEqualitiesProperty() {
    return validStateEqualitiesProperty;
  }

  public ListProperty<String> invalidStateEqualitiesProperty() {
    return invalidStateEqualitiesProperty;
  }
}
