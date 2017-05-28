package de.hhu.stups.bsynthesis.ui.components;

import de.prob.statespace.Trace;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ModelCheckingResult {

  private final ObjectProperty<Trace> traceProperty = new SimpleObjectProperty<>();

  public ModelCheckingResult(final Trace trace) {
    traceProperty.set(trace);
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
}
