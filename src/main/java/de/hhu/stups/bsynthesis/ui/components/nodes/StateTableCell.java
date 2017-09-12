package de.hhu.stups.bsynthesis.ui.components.nodes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * State table cell.
 */
class StateTableCell {
  private final StringProperty varName;
  private final StringProperty inputState;
  private final BooleanProperty ignoreVar;

  StateTableCell(final String varName,
                 final String inputState,
                 final BooleanProperty ignoreVar) {
    this.varName = new SimpleStringProperty(this, "varName", varName);
    this.inputState = new SimpleStringProperty(this, "inputState", inputState);
    this.ignoreVar = new SimpleBooleanProperty();
    if (ignoreVar != null) {
      this.ignoreVar.bindBidirectional(ignoreVar);
    }
  }

  String getVarName() {
    return varName.get();
  }

  ReadOnlyStringProperty varNameProperty() {
    return varName;
  }

  String getInputState() {
    return inputState.get();
  }

  void setInputState(final String inputState) {
    this.inputState.set(inputState);
  }

  ReadOnlyStringProperty inputStateProperty() {
    return inputState;
  }

  BooleanProperty ignoreVarProperty() {
    return ignoreVar;
  }
}
