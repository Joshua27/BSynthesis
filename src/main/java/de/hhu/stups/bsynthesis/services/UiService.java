package de.hhu.stups.bsynthesis.services;

import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.hhu.stups.bsynthesis.ui.controller.ControllerTab;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.reactfx.EventSource;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UiService {

  private final EventSource<ControllerTab> showTabEventStream;
  private final EventSource<UiZoom> zoomEventStream;
  private final NodeContextMenuFactory nodeContextMenuFactory;
  private final StateNodeFactory stateNodeFactory;
  private final TransitionNodeFactory transitionNodeFactory;
  private final BooleanProperty zoomInEnabledProperty;
  private final BooleanProperty zoomOutEnabledProperty;
  private final MapProperty<String, BooleanProperty> currentVarStatesMapProperty;

  /**
   * Initialize node factories and event sources.
   */
  @Inject
  public UiService(final NodeContextMenuFactory nodeContextMenuFactory,
                   final StateNodeFactory stateNodeFactory,
                   final TransitionNodeFactory transitionNodeFactory) {
    this.nodeContextMenuFactory = nodeContextMenuFactory;
    this.stateNodeFactory = stateNodeFactory;
    this.transitionNodeFactory = transitionNodeFactory;
    currentVarStatesMapProperty = new SimpleMapProperty<>(FXCollections.observableHashMap());
    showTabEventStream = new EventSource<>();
    zoomEventStream = new EventSource<>();
    zoomInEnabledProperty = new SimpleBooleanProperty();
    zoomOutEnabledProperty = new SimpleBooleanProperty();
  }

  /**
   * Initialize {@link #currentVarStatesMapProperty} storing a boolean property for each variable
   * stating whether this variable should be ignored during synthesis or not.
   */
  public void initializeCurrentVarBindings(final ObservableSet<String> machineVarNames) {
    currentVarStatesMapProperty.clear();
    machineVarNames.forEach(machineVarName ->
        currentVarStatesMapProperty.put(machineVarName, new SimpleBooleanProperty(false)));
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

  public EventSource<ControllerTab> showTabEventStream() {
    return showTabEventStream;
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

  public enum UiZoom {
    ZOOM_IN, ZOOM_OUT;

    public boolean isZoomIn() {
      return this.equals(ZOOM_IN);
    }
  }
}
