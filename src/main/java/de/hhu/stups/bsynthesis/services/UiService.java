package de.hhu.stups.bsynthesis.services;

import de.hhu.stups.bsynthesis.ui.components.VisualizeBehavior;
import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeLine;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.fxmisc.easybind.EasyBind;
import org.reactfx.EventSource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UiService {

  private final EventSource<ApplicationEvent> applicationEventStream;
  private final EventSource<UiZoom> zoomEventStream;
  private final EventSource<ValidationPaneEvent> validationPaneEventSource;
  private final EventSource<NodeLine> addNodeConnectionEventSource;
  private final EventSource<MachineVisualization> visualizeBehaviorEventSource;

  private final NodeContextMenuFactory nodeContextMenuFactory;
  private final StateNodeFactory stateNodeFactory;
  private final TransitionNodeFactory transitionNodeFactory;

  private final BooleanProperty zoomInEnabledProperty;
  private final BooleanProperty zoomOutEnabledProperty;
  private final MapProperty<String, BooleanProperty> currentVarStatesMapProperty;

  private final VisualizeBehavior visualizeBehavior;

  /**
   * Initialize node factories and event sources.
   */
  @Inject
  public UiService(final NodeContextMenuFactory nodeContextMenuFactory,
                   final StateNodeFactory stateNodeFactory,
                   final TransitionNodeFactory transitionNodeFactory,
                   final VisualizeBehavior visualizeBehavior) {
    this.nodeContextMenuFactory = nodeContextMenuFactory;
    this.stateNodeFactory = stateNodeFactory;
    this.transitionNodeFactory = transitionNodeFactory;
    this.visualizeBehavior = visualizeBehavior;

    currentVarStatesMapProperty = new SimpleMapProperty<>(FXCollections.observableHashMap());
    validationPaneEventSource = new EventSource<>();
    applicationEventStream = new EventSource<>();
    zoomEventStream = new EventSource<>();
    zoomInEnabledProperty = new SimpleBooleanProperty();
    zoomOutEnabledProperty = new SimpleBooleanProperty();
    addNodeConnectionEventSource = new EventSource<>();
    visualizeBehaviorEventSource = new EventSource<>();
    visualizeBehaviorEventSource.subscribe(this::handleMachineVisualization);

    EasyBind.subscribe(visualizeBehavior.ignoredIDsProperty(), ignoredIDs ->
        new Thread(() -> {
          final Map<String, BooleanProperty> currentVarStatesMap =
              currentVarStatesMapProperty.get();
          ignoredIDs.forEach(ignoredID -> currentVarStatesMap.get(ignoredID).set(true));
        }).start());
  }

  private void handleMachineVisualization(final MachineVisualization machineVisualization) {
    if (machineVisualization.getVisualizationType().isInvariant()) {
      DaemonThread.getDaemonThread(() -> {
        final Map<String, Set<StateNode>> stateNodes =
            visualizeBehavior.visualizeInvariants(stateNodeFactory);
        stateNodes.get("valid").forEach(this::showNode);
        stateNodes.get("invalid").forEach(this::showNode);
      }).start();
    } else {
      DaemonThread.getDaemonThread(() -> {
        final Map<String, Set<TransitionNode>> transitionNodes =
            visualizeBehavior.visualizeOperation(machineVisualization.getOperationName(),
                transitionNodeFactory);
        transitionNodes.get("valid").forEach(this::showNode);
        transitionNodes.get("invalid").forEach(this::showNode);
      }).start();
    }
  }

  private void showNode(final BasicNode basicNode) {
    validationPaneEventSource.push(
        new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, basicNode));
  }

  /**
   * Initialize {@link #currentVarStatesMapProperty} storing a boolean property for each variable
   * stating whether this variable should be ignored during synthesis or not.
   */
  public void initializeCurrentVarBindings(final ObservableSet<String> machineVarNames) {
    currentVarStatesMapProperty.clear();
    machineVarNames.forEach(machineVarName ->
        currentVarStatesMapProperty.put(machineVarName, new SimpleBooleanProperty(false)));
    currentVarStatesMapProperty.values().forEach(booleanProperty ->
        EasyBind.subscribe(booleanProperty, aBoolean -> {
          if (aBoolean) {
            doNotIgnoreVarIfAllIgnored(booleanProperty);
          }
        }));
  }

  /**
   * If a machine variable is set to be ignored check if at least one machine variable is
   * considered. Otherwise, do not allow to ignore this variable.
   */
  private void doNotIgnoreVarIfAllIgnored(final BooleanProperty ignoreVarProperty) {
    final Optional<BooleanProperty> optionalConsideredVar = currentVarStatesMapProperty.values()
        .stream().filter(booleanProperty -> !booleanProperty.get()).findAny();
    if (!optionalConsideredVar.isPresent()) {
      ignoreVarProperty.set(false);
    }
  }

  public BooleanProperty zoomInEnabledProperty() {
    return zoomInEnabledProperty;
  }

  public BooleanProperty zoomOutEnabledProperty() {
    return zoomOutEnabledProperty;
  }

  public NodeContextMenuFactory getNodeContextMenuFactory() {
    return nodeContextMenuFactory;
  }

  public StateNodeFactory getStateNodeFactory() {
    return stateNodeFactory;
  }

  public TransitionNodeFactory getTransitionNodeFactory() {
    return transitionNodeFactory;
  }

  public EventSource<ApplicationEvent> applicationEventStream() {
    return applicationEventStream;
  }

  public EventSource<UiZoom> zoomEventStream() {
    return zoomEventStream;
  }

  public MapProperty<String, BooleanProperty> currentVarStatesMapProperty() {
    return currentVarStatesMapProperty;
  }

  /**
   * Return the machine variable names from {@link #currentVarStatesMapProperty} that are not set to
   * be ignored.
   */
  public Set<String> getCurrentVarNames() {
    return currentVarStatesMapProperty().keySet().stream().filter(varName ->
        !currentVarStatesMapProperty.get(varName).get()).collect(Collectors.toSet());
  }

  /**
   * Reset the {@link #currentVarStatesMapProperty} to consider all machine variables, i.e., set
   * each binding stating to ignore a machine variable to false.
   */
  public void resetCurrentVarBindings() {
    currentVarStatesMapProperty.forEach((machineVarname, booleanProperty) ->
        currentVarStatesMapProperty.get(machineVarname).set(false));
  }

  public EventSource<ValidationPaneEvent> validationPaneEventSource() {
    return validationPaneEventSource;
  }

  public EventSource<NodeLine> addNodeConnectionEventSource() {
    return addNodeConnectionEventSource;
  }

  public EventSource<MachineVisualization> visualizeBehaviorEventSource() {
    return visualizeBehaviorEventSource;
  }
}
