package de.hhu.stups.bsynthesis.prob;

import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Variable;
import de.prob.statespace.StateSpace;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

public interface BMachineMisc {
  /**
   * Return a set of the current machine variables for a given {@link StateSpace}.
   */
  static ObservableSet<String> getMachineVars(final StateSpace stateSpace) {
    final ObservableSet<String> variableNames = FXCollections.observableSet();
    final AbstractElement mainComponent = stateSpace.getMainComponent();
    mainComponent.getChildrenOfType(Variable.class)
        .forEach(variable -> variableNames.add(variable.getName()));
    return variableNames;
  }
}
