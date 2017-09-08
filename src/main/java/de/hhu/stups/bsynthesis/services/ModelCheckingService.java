package de.hhu.stups.bsynthesis.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.ui.components.DeadlockRepair;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingResult;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckErrorUncovered;
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

import org.fxmisc.easybind.EasyBind;
import org.reactfx.EventSource;

/**
 * Checks a model when a {@link StateSpace} is pushed to {@link #stateSpaceEventStream}.
 * Set {@link #runningProperty()} false to to stop the model checker. Properties {@link
 * #resultProperty()} and {@link #stateSpaceStatsProperty()} for observation.
 */
@Singleton
public class ModelCheckingService implements IModelCheckListener {

  private final BooleanProperty runningProperty;
  private final ObjectProperty<ModelCheckingResult> resultProperty;
  private final EventSource<StateSpace> stateSpaceEventStream;
  private final ObjectProperty<StateSpaceStats> stateSpaceStatsProperty;
  private final IntegerProperty processedNodesProperty;
  private final IntegerProperty totalNodesProperty;
  private final BooleanProperty indicatorPresentProperty;
  private final ObjectProperty<Trace> errorTraceProperty;
  private final ObjectProperty<DeadlockRepair> deadlockRepairProperty;

  private ModelChecker checker;
  private IModelCheckJob currentJob;

  /**
   * Initialize the properties. Set listeners to {@link #stopModelChecking() stop model checking} as
   * soon as {@link #runningProperty()} is set to false. Start model checking by pushing a {@link
   * StateSpace} to the {@link #stateSpaceEventStream}.
   */
  @Inject
  public ModelCheckingService() {
    runningProperty = new SimpleBooleanProperty(false);
    resultProperty = new SimpleObjectProperty<>();
    stateSpaceStatsProperty = new SimpleObjectProperty<>();
    indicatorPresentProperty = new SimpleBooleanProperty();
    processedNodesProperty = new SimpleIntegerProperty(0);
    totalNodesProperty = new SimpleIntegerProperty(0);
    errorTraceProperty = new SimpleObjectProperty<>();
    stateSpaceEventStream = new EventSource<>();
    deadlockRepairProperty = new SimpleObjectProperty<>();

    EasyBind.subscribe(runningProperty, aBoolean -> {
      if (!aBoolean) {
        stopModelChecking();
      }
    });

    stateSpaceEventStream.subscribe(stateSpace -> {
      if (stateSpace != null) {
        runningProperty().set(true);
        final IModelCheckJob modelCheckingJob =
            new ConsistencyChecker(stateSpace, ModelCheckingOptions.DEFAULT, null, this);
        currentJob = modelCheckingJob;
        checker = new ModelChecker(modelCheckingJob);
        checker.start();
      }
    });

    EasyBind.subscribe(stateSpaceStatsProperty, stateSpaceStats -> {
      if (stateSpaceStats == null) {
        return;
      }
      Platform.runLater(() -> {
        processedNodesProperty.set(stateSpaceStats.getNrProcessedNodes());
        totalNodesProperty.set(stateSpaceStats.getNrTotalNodes());
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
      // error found
      final ModelCheckErrorUncovered errorUncovered = (ModelCheckErrorUncovered) result;
      final StateSpace s = checker.getStateSpace();
      Platform.runLater(() -> {
        final Trace trace = ((ITraceDescription) result).getTrace(s);
        errorTraceProperty.set(trace);
        resultProperty.set(new ModelCheckingResult(trace,
            UncoveredError.getUncoveredErrorFromMessage(errorUncovered.getMessage())));
        runningProperty.set(false);
      });
      return;
    }
    if (stats.getNrProcessedNodes() == stats.getNrTotalNodes()) {
      // the model has been checked completely and no error has been found
      Platform.runLater(() -> {
        errorTraceProperty.set(null);
        resultProperty.set(new ModelCheckingResult(null));
        runningProperty.set(false);
      });
    }
  }

  private void stopModelChecking() {
    if (checker != null) {
      checker.cancel();
    }
    if (currentJob != null) {
      currentJob.getStateSpace().sendInterrupt();
    }
  }

  public BooleanProperty runningProperty() {
    return runningProperty;
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

  /**
   * Reset the service by setting all properties to their default values.
   */
  public void reset() {
    runningProperty.set(false);
    resultProperty.set(null);
    stateSpaceStatsProperty.set(null);
    errorTraceProperty.set(null);
    indicatorPresentProperty.set(false);
    deadlockRepairProperty.set(null);
  }

  public ObjectProperty<Trace> errorTraceProperty() {
    return errorTraceProperty;
  }

  public ObjectProperty<DeadlockRepair> deadlockRepairProperty() {
    return deadlockRepairProperty;
  }

  public EventSource<StateSpace> stateSpaceEventStream() {
    return stateSpaceEventStream;
  }
}
