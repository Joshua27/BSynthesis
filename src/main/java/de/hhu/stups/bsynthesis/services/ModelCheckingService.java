package de.hhu.stups.bsynthesis.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.ui.components.ModelCheckingResult;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelChecker;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Checks a model as soon as {@link #stateSpaceProperty()} is set to a non null {@link StateSpace}
 * object. Set {@link #runningProperty()} false to to stop the model checker. Properties {@link
 * #resultProperty()} and {@link #stateSpaceStatsProperty()} for observation.
 */
@Singleton
public class ModelCheckingService implements IModelCheckListener {

  private final BooleanProperty runningProperty;
  private final ObjectProperty<ModelCheckingResult> resultProperty;
  private final ObjectProperty<StateSpace> stateSpaceProperty;
  private final ObjectProperty<StateSpaceStats> stateSpaceStatsProperty;
  private final IntegerProperty processedNodesProperty;
  private final IntegerProperty totalNodesProperty;
  private final BooleanProperty indicatorPresentProperty;
  private final ObjectProperty<Trace> errorFoundProperty;

  private ModelChecker checker;

  @Inject
  public ModelCheckingService() {
    runningProperty = new SimpleBooleanProperty(false);
    resultProperty = new SimpleObjectProperty<>();
    stateSpaceProperty = new SimpleObjectProperty<>();
    stateSpaceStatsProperty = new SimpleObjectProperty<>();
    indicatorPresentProperty = new SimpleBooleanProperty();
    processedNodesProperty = new SimpleIntegerProperty(0);
    totalNodesProperty = new SimpleIntegerProperty(0);
    errorFoundProperty = new SimpleObjectProperty<>();

    runningProperty.addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        stopModelChecking();
      }
    });

    stateSpaceProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        runningProperty().set(true);
        checker = new ModelChecker(
            new ConsistencyChecker(newValue, ModelCheckingOptions.DEFAULT, null, this));
        checker.start();
      }
    });

    stateSpaceStatsProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      Platform.runLater(() -> {
        processedNodesProperty.set(newValue.getNrProcessedNodes());
        totalNodesProperty.set(newValue.getNrTotalNodes());
      });
    });
  }

  @Override
  public void updateStats(final String jobId,
                          final long timeElapsed,
                          final IModelCheckingResult result,
                          final StateSpaceStats stats) {
    stateSpaceStatsProperty.set(stats);
  }

  @Override
  public void isFinished(final String jobId,
                         final long timeElapsed,
                         final IModelCheckingResult result,
                         final StateSpaceStats stats) {
    if (result instanceof ITraceDescription) {
      // TODO: differ between invariant violation or deadlock (and error in initialization state?)
      // error found
      final StateSpace s = checker.getStateSpace();
      Platform.runLater(() -> {
        final Trace trace = ((ITraceDescription) result).getTrace(s);
        resultProperty.set(new ModelCheckingResult(trace));
        errorFoundProperty.set(trace);
        runningProperty.set(false);
      });
      return;
    }
    if (stats.getNrProcessedNodes() == stats.getNrTotalNodes()) {
      // the model has been checked completely
      Platform.runLater(() -> {
        errorFoundProperty.set(null);
        resultProperty.set(new ModelCheckingResult(null));
        runningProperty.set(false);
      });
    }
  }

  private void stopModelChecking() {
    checker.cancel();
  }

  public BooleanProperty runningProperty() {
    return runningProperty;
  }

  public ObjectProperty<StateSpace> stateSpaceProperty() {
    return stateSpaceProperty;
  }

  public ObjectProperty<ModelCheckingResult> resultProperty() {
    return resultProperty;
  }

  public ObjectProperty<StateSpaceStats> stateSpaceStatsProperty() {
    return stateSpaceStatsProperty;
  }

  public IntegerProperty processedNodesProperty() {
    return processedNodesProperty;
  }

  public IntegerProperty totalNodesProperty() {
    return totalNodesProperty;
  }

  public BooleanProperty indicatorPresentProperty() {
    return indicatorPresentProperty;
  }

  public void reset() {
    runningProperty.set(false);
    resultProperty.set(null);
    stateSpaceProperty.set(null);
    stateSpaceStatsProperty.set(null);
    errorFoundProperty.set(null);
    indicatorPresentProperty.set(false);
  }

  public ObjectProperty<Trace> errorFoundProperty() {
    return errorFoundProperty;
  }
}
