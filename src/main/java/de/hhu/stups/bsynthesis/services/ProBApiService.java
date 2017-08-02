package de.hhu.stups.bsynthesis.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.prob.DistinguishingExample;
import de.hhu.stups.bsynthesis.prob.ResetSynthesisCommand;
import de.hhu.stups.bsynthesis.prob.StartSynthesisCommand;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.stage.FileChooser;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

/**
 * A service providing a {@link #mainStateSpaceProperty main statespace} as well as an amount of
 * {@link #INSTANCES} further {@link StateSpace statespaces}. We run computations on the
 * {@link #stateSpacesProperty further instances} and use the
 * {@link #mainStateSpaceProperty main statespace} for synchronization in case we find a solution.
 * All {@link StateSpace statespaces} have loaded the same model and are in the same state in case
 * synthesis is not running and has not been suspended.
 */
@Singleton
public class ProBApiService {

  private static final int INSTANCES = 4;

  private final ExecutorService threadPoolExecutor = Executors.newWorkStealingPool(INSTANCES);
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final EventSource<StartSynthesisCommand> startSynthesisEventSource;
  private final ObjectProperty<StateSpace> mainStateSpaceProperty;
  private final MapProperty<Task<Void>, StateSpace> synthesisTasksMap;
  private final SetProperty<StateSpace> stateSpacesProperty;
  private final MapProperty<StateSpace, Integer> suspendedStateSpacesMap;
  private final BooleanProperty synthesisSucceededProperty;
  private final BooleanProperty synthesisRunningProperty;
  private final StringProperty modifiedMachineCodeProperty;
  private final Api proBApi;
  private final UiService uiService;
  private final Queue<StateSpace> idleStateSpaceQueue;
  private final IntegerProperty currentLibraryExpansionProperty;

  /**
   * Initialize properties and the injected {@link Api}.
   */
  @Inject
  public ProBApiService(final Api proBApi,
                        final UiService uiService) {
    this.proBApi = proBApi;
    this.uiService = uiService;
    startSynthesisEventSource = new EventSource<>();
    startSynthesisEventSource.subscribe(this::startSynthesis);
    mainStateSpaceProperty = new SimpleObjectProperty<>();
    stateSpacesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    synthesisSucceededProperty = new SimpleBooleanProperty(false);
    synthesisRunningProperty = new SimpleBooleanProperty(false);
    modifiedMachineCodeProperty = new SimpleStringProperty();
    synthesisTasksMap = new SimpleMapProperty<>(FXCollections.observableHashMap());
    idleStateSpaceQueue = new LinkedBlockingQueue<>();
    currentLibraryExpansionProperty = new SimpleIntegerProperty();
    suspendedStateSpacesMap = new SimpleMapProperty<>(FXCollections.observableHashMap());
  }

  /**
   * Show a {@link FileChooser dialog} and open a machine from file.
   */
  public SpecificationType loadMachine(final File file) {
    if (file == null) {
      return null;
    }
    final StateSpace stateSpace = loadStateSpace(file);
    if (stateSpace == null) {
      return null;
    }
    mainStateSpaceProperty.set(stateSpace);
    stateSpacesProperty.clear();
    idleStateSpaceQueue.clear();
    // load the same model to several instances in a background thread
    final Thread loadInstancesThread = new Thread(() ->
        IntStream.range(0, INSTANCES).forEach(value -> {
          final StateSpace newStateSpace = loadStateSpace(file);
          if (newStateSpace != null) {
            stateSpacesProperty.add(newStateSpace);
            idleStateSpaceQueue.add(newStateSpace);
          }
        }));
    loadInstancesThread.setDaemon(true);
    loadInstancesThread.start();
    return hasClassicalBExtension(file) ? SpecificationType.CLASSICAL_B : SpecificationType.EVENT_B;
  }

  private StateSpace loadStateSpace(final File file) {
    try {
      if (hasClassicalBExtension(file)) {
        return proBApi.b_load(file.getPath());
      } else {
        return proBApi.eventb_load(file.getPath());
      }
    } catch (final IOException exception) {
      logger.error("IOException while loading " + file.getPath(), exception);
    } catch (final ModelTranslationError modelTranslationError) {
      logger.error("Translation error while loading " + file.getPath(),
          modelTranslationError);
    }
    return null;
  }

  private boolean hasClassicalBExtension(final File file) {
    return ".mch".equals(file.getName().substring(file.getName().lastIndexOf('.')));
  }

  public ObjectProperty<StateSpace> mainStateSpaceProperty() {
    return mainStateSpaceProperty;
  }

  /**
   * Synchronize the {@link #stateSpacesProperty} with the {@link #mainStateSpaceProperty}.
   */
  public void synchronizeStateSpaces() {
    stateSpacesProperty.clear();
    IntStream.range(0, INSTANCES).forEach(value ->
        stateSpacesProperty.add(mainStateSpaceProperty.get()));
    logger.info("Synchronized statespaces in the ProBApiService used for synthesis.");
  }

  public StateSpace getMainStateSpace() {
    return mainStateSpaceProperty.get();
  }

  public EventSource<StartSynthesisCommand> startSynthesisEventSource() {
    return startSynthesisEventSource;
  }

  public BooleanProperty synthesisRunningProperty() {
    return synthesisRunningProperty;
  }

  BooleanProperty synthesisSucceededProperty() {
    return synthesisSucceededProperty;
  }

  private void startSynthesis(final StartSynthesisCommand startSynthesisCommand) {
    currentLibraryExpansionProperty.set(startSynthesisCommand.getLibraryExpansion());
    synthesisRunningProperty.set(true);
    // the user selected the library components, and thus, we just start one instance using
    // this configuration
    if (!startSynthesisCommand.isDefaultLibraryConfiguration()) {
      startSynthesisSingleInstance(startSynthesisCommand);
      return;
    }
    // otherwise, we run several instances of synthesis with different library configurations
    startSynthesisParallel(startSynthesisCommand);
  }

  private void startSynthesisParallel(final StartSynthesisCommand startSynthesisCommand) {
    logger.info("Starting synthesis on several instances. "
        + "Available statespace instances: {}", idleStateSpaceQueue);
    IntStream.range(0, INSTANCES).forEach(value -> {
      final StartSynthesisCommand copiedCommand = new StartSynthesisCommand(startSynthesisCommand);
      final StateSpace stateSpace;
      if (suspendedStateSpacesMap.isEmpty()) {
        // start a new instance with the current library expansion
        copiedCommand.setLibraryExpansion(currentLibraryExpansionProperty.get());
        stateSpace = idleStateSpaceQueue.poll();
        if (stateSpace == null) {
          // TODO: save this startsynthesiscommand if it could not be executed right now?
          return;
        }
        logger.info("Start synthesis instance {}.", value);
      } else {
        // or restart a suspended statespace with its specific library expansion
        final Map.Entry<StateSpace, Integer> suspendedEntry =
            suspendedStateSpacesMap.entrySet().iterator().next();
        copiedCommand.setLibraryExpansion(suspendedEntry.getValue());
        stateSpace = suspendedEntry.getKey();
        suspendedStateSpacesMap.remove(suspendedEntry.getKey());
      }
      final Task<Void> synthesisTask = getSynthesisTask(stateSpace, copiedCommand);
      synthesisTasksMap.put(synthesisTask, stateSpace);
      threadPoolExecutor.execute(synthesisTask);
      startSynthesisCommand.expandLibrary();
      currentLibraryExpansionProperty.set(startSynthesisCommand.getLibraryExpansion());
    });
  }

  private void startSynthesisSingleInstance(final StartSynthesisCommand startSynthesisCommand) {
    logger.info(
        "Start a single synthesis instance using the library components selected by the user.");
    final StateSpace stateSpace = idleStateSpaceQueue.poll();
    if (stateSpace == null) {
      logger.error("No statespace available when trying to run a single synthesis instance.");
      return;
    }
    final Task<Void> synthesisTask = getSynthesisTask(stateSpace, startSynthesisCommand);
    synthesisTasksMap.put(synthesisTask, stateSpace);
    threadPoolExecutor.execute(synthesisTask);
  }

  /**
   * Restart synthesis if no other task succeeded by now and we have not reached the maximum amount
   * of library expansions yet. The distinguishing examples probably found by other instances
   * running in parallel are not considered here. The user has to validate each example
   * and manually restart synthesis which then will find the corresponding {@link StateSpace} using
   * {@link #suspendedStateSpacesMap} and then also considers the additional examples.
   */
  private void expandLibraryAndRestartSynthesis(final StartSynthesisCommand startSynthesisCommand) {
    startSynthesisCommand.setLibraryExpansion(currentLibraryExpansionProperty.get());
    if (synthesisSucceededProperty.not().get() && startSynthesisCommand.expandLibrary()) {
      logger.info("Expand library to level " + startSynthesisCommand.getLibraryExpansion());
      final StateSpace stateSpace = idleStateSpaceQueue.poll();
      if (stateSpace == null) {
        // TODO: save this startsynthesiscommand if it could not be executed right now?
        return;
      }
      final Task<Void> synthesisTask = getSynthesisTask(stateSpace, startSynthesisCommand);
      synthesisTasksMap.put(synthesisTask, stateSpace);
      threadPoolExecutor.execute(synthesisTask);
      currentLibraryExpansionProperty.set(startSynthesisCommand.getLibraryExpansion());
    }
  }

  private void handleDistinguishingExample(final SynthesisType synthesisType,
                                           final StateSpace stateSpace,
                                           final DistinguishingExample distinguishingExample) {
    addStateSpaceToQueue(stateSpace);
    if (synthesisSucceededProperty.get() || stateSpace == null) {
      return;
    }
    synthesisRunningProperty().set(false);
    final FindStateCommand inputStateCommand = new FindStateCommand(
        stateSpace, new ClassicalB(distinguishingExample.getInputStateEquality()), false);
    stateSpace.execute(inputStateCommand);
    final State inputState = stateSpace.getState(inputStateCommand.getStateId());
    if (handleDistinguishingTransition(synthesisType, stateSpace, inputState,
        distinguishingExample)) {
      return;
    }
    handleDistinguishingState(stateSpace, inputState);
  }

  private void handleDistinguishingState(final StateSpace stateSpace,
                                         final State inputState) {
    if (synthesisSucceededProperty.get()) {
      return;
    }
    final Point2D distinguishingNodePosition =
        new Point2D(ValidationPane.WIDTH / 2, ValidationPane.HEIGHT / 2);
    final StateNode stateNode = uiService.getStateNodeFactory()
        .create(inputState, stateSpace.getTrace(inputState.getId()), distinguishingNodePosition,
            NodeState.TENTATIVE);
    initializeDistNode(stateNode);
    uiService.showNodeEventSource().push(stateNode);
  }

  private boolean handleDistinguishingTransition(final SynthesisType synthesisType,
                                                 final StateSpace stateSpace,
                                                 final State inputState,
                                                 final DistinguishingExample distExample) {
    if (synthesisSucceededProperty.get()) {
      return false;
    }
    final Point2D distinguishingNodePosition =
        new Point2D(ValidationPane.WIDTH / 2, ValidationPane.HEIGHT / 2);
    if (!distExample.getOutputTuples().isEmpty()) {
      final FindStateCommand outputStateCommand = new FindStateCommand(
          stateSpace, new ClassicalB(distExample.getOutputStateEquality()), false);
      stateSpace.execute(outputStateCommand);
      final State outputState = stateSpace.getState(outputStateCommand.getStateId());
      final TransitionNode transitionNode = uiService.getTransitionNodeFactory()
          .create(inputState, outputState, distinguishingNodePosition, NodeState.TENTATIVE);
      initializeDistNode(transitionNode);
      uiService.showNodeEventSource().push(transitionNode);
      return true;
    }
    if (synthesisType.isAction()) {
      // synthesizing an action but simultaneous synthesis of an appropriate guard returned a
      // distinguishing state, therefore we set the transitions input and output state to be equal:
      // if the state should be negative we only need the input, otherwise the user has to adapt
      // the output anyway
      final TransitionNode transitionNode = uiService.getTransitionNodeFactory()
          .create(inputState, inputState, distinguishingNodePosition, NodeState.TENTATIVE);
      uiService.showNodeEventSource().push(transitionNode);
      initializeDistNode(transitionNode);
      return true;
    }
    return false;
  }

  private void initializeDistNode(final BasicNode basicNode) {
    Platform.runLater(() -> {
      basicNode.isExpandedProperty().set(true);
      if (basicNode instanceof StateNode) {
        final StateNode stateNode = (StateNode) basicNode;
        stateNode.validateState();
        stateNode.stateFromModelCheckingProperty().set(true);
        return;
      }
      ((TransitionNode) basicNode).validateTransition();
    });
  }

  /**
   * Create a synthesis task if an idle statespace is given.
   */
  private Task<Void> getSynthesisTask(final StateSpace stateSpace,
                                      final StartSynthesisCommand startSynthesisCommand) {
    final Task<Void> synthesisTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        if (synthesisSucceededProperty.get()) {
          return null;
        }
        startSynthesisCommand.distinguishingExampleProperty()
            .addListener((observable, oldValue, newValue) -> {
              if (newValue != null && !newValue.equals(oldValue)) {
                handleDistinguishingExample(
                    startSynthesisCommand.getSynthesisType(), stateSpace, newValue);
                // suspend this statespace when finding a distinguishing example to revisit the same
                // synthesis context when restarting synthesis after validating the example
                suspendedStateSpacesMap.put(
                    stateSpace, startSynthesisCommand.getLibraryExpansion());
              }
            });
        // use the succeeded property of the command and do not register setOnSucceeded()
        // of the task since ProB might raise an integer overflow error but in prolog we then
        // fall back to other constraint solvers like Z3 so that synthesis might succeeds although
        // the task itself fails due to raising an exception
        startSynthesisCommand.synthesisSucceededProperty().addListener(
            (observable, oldValue, newValue) -> {
              if (newValue && synthesisSucceededProperty.not().get()
                  && modifiedMachineCodeProperty.get() == null) {
                synthesisRunningProperty.set(false);
                synthesisSucceededProperty.set(true);
                synthesisTasksMap.remove(this);
                cancelRunningTasks();
                modifiedMachineCodeProperty.set(
                    startSynthesisCommand.modifiedMachineCodeProperty().get());
              }
              resetSynthesisContextForStatespace(stateSpace);
            });
        stateSpace.execute(startSynthesisCommand);
        return null;
      }
    };
    setSynthesisTaskListener(synthesisTask, stateSpace, startSynthesisCommand);
    return synthesisTask;
  }

  private void setSynthesisTaskListener(final Task<Void> synthesisTask,
                                        final StateSpace stateSpace,
                                        final StartSynthesisCommand startSynthesisCommand) {
    synthesisTask.setOnCancelled(event -> {
      sendInterruptIfBusy(stateSpace);
      synthesisTasksMap.remove(synthesisTask);
      if (synthesisTasksMap.values().isEmpty()) {
        synthesisRunningProperty.set(false);
      }
    });
    synthesisTask.setOnFailed(event -> {
      synthesisTasksMap.remove(synthesisTask);
      if (synthesisTasksMap.values().isEmpty()) {
        synthesisRunningProperty.set(false);
      }
      if (stateSpace == null) {
        return;
      }
      addStateSpaceToQueue(stateSpace);
      resetSynthesisContextForStatespace(stateSpace);
      expandLibraryAndRestartSynthesis(startSynthesisCommand);
    });
  }

  /**
   * Interrupt the given {@link StateSpace} if it is busy.
   */
  private void sendInterruptIfBusy(final StateSpace stateSpace) {
    addStateSpaceToQueue(stateSpace);
    if (stateSpace == null) {
      return;
    }
    if (!stateSpace.isBusy()) {
      return;
    }
    final Thread interruptThread = new Thread(() -> {
      stateSpace.sendInterrupt();
      stateSpace.execute(new ResetSynthesisCommand());
    });
    interruptThread.setDaemon(true);
    interruptThread.start();
  }

  private void addStateSpaceToQueue(final StateSpace stateSpace) {
    if (stateSpace == null || idleStateSpaceQueue.contains(stateSpace)) {
      return;
    }
    idleStateSpaceQueue.add(stateSpace);
  }

  /**
   * Cancel all {@link #synthesisTasksMap running tasks}.
   */
  private void cancelRunningTasks() {
    synthesisTasksMap.keySet().forEach(voidTask -> {
      final Thread cancelRunningTaskThread = new Thread(() -> voidTask.cancel(true));
      cancelRunningTaskThread.setDaemon(true);
      cancelRunningTaskThread.start();
    });
    suspendedStateSpacesMap.clear();
    synthesisRunningProperty.set(false);
  }

  StringProperty modifiedMachineCodeProperty() {
    return modifiedMachineCodeProperty;
  }

  /**
   * Reset properties.
   */
  public void reset() {
    cancelRunningTasks();
    synthesisSucceededProperty.set(false);
    modifiedMachineCodeProperty.set(null);
    currentLibraryExpansionProperty.set(1);
    suspendedStateSpacesMap.clear();
    stateSpacesProperty.forEach(this::resetSynthesisContextForStatespace);
  }

  private void resetSynthesisContextForStatespace(final StateSpace stateSpace) {
    final Thread resetSynthesisContextThread = new Thread(() ->
        stateSpace.execute(new ResetSynthesisCommand()));
    resetSynthesisContextThread.setDaemon(true);
    resetSynthesisContextThread.start();
  }

  public void shutdownExecutor() {
    threadPoolExecutor.shutdown();
  }
}