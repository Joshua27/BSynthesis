package de.hhu.stups.bsynthesis.ui.components;

import de.hhu.stups.bsynthesis.services.UncoveredError;
import de.prob.statespace.Trace;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ModelCheckingResult {

  private final ObjectProperty<Trace> traceProperty = new SimpleObjectProperty<>();
  private final UncoveredError uncoveredError;

  public ModelCheckingResult(final Trace trace) {
    traceProperty.set(trace);
    uncoveredError = null;
  }

  public ModelCheckingResult(final Trace trace,
                             final UncoveredError uncoveredError) {
    traceProperty.set(trace);
    this.uncoveredError = uncoveredError;
  }

  public boolean errorFound() {
    return traceProperty.isNotNull().get();
  }

  public Trace getTrace() {
    return traceProperty.get();
  }

  public ObjectProperty<Trace> traceProperty() {
    return traceProperty;
  }

  public UncoveredError getUncoveredError() {
    return uncoveredError;
  }
}
