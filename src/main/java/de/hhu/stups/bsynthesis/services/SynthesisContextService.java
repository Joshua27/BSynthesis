package de.hhu.stups.bsynthesis.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Variable;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.StateSpace;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * A service providing several properties describing the current synthesis context, i.e. the
 * currently loaded machine's information, results from model checking.
 */
@Singleton
public class SynthesisContextService {

  private final NodeContextMenuFactory nodeContextMenuFactory;
  private final StateNodeFactory stateNodeFactory;
  private final TransitionNodeFactory transitionNodeFactory;

  private final SetProperty<String> machineVarNamesProperty;
  private final ObjectProperty<SynthesisType> synthesisTypeProperty;
  private final ObjectProperty<StateSpace> stateSpaceProperty;
  private final StringProperty currentOperationProperty;
  private final BooleanProperty invariantViolatedProperty;
  private final BooleanProperty showLibraryConfigurationProperty;
  private final ObjectProperty<AnimationSelector> animationSelectorProperty;
  private final BooleanProperty showSynthesisTabProperty;

  /**
   * Initialize all properties and set the injected factories.
   */
  @Inject
  public SynthesisContextService(final NodeContextMenuFactory nodeContextMenuFactory,
                                 final StateNodeFactory stateNodeFactory,
                                 final TransitionNodeFactory transitionNodeFactory) {
    this.nodeContextMenuFactory = nodeContextMenuFactory;
    this.stateNodeFactory = stateNodeFactory;
    this.transitionNodeFactory = transitionNodeFactory;
    machineVarNamesProperty = new SimpleSetProperty<>();
    synthesisTypeProperty = new SimpleObjectProperty<>();
    stateSpaceProperty = new SimpleObjectProperty<>();
    animationSelectorProperty = new SimpleObjectProperty<>(new AnimationSelector());
    currentOperationProperty = new SimpleStringProperty();
    invariantViolatedProperty = new SimpleBooleanProperty(false);
    showLibraryConfigurationProperty = new SimpleBooleanProperty(false);
    showSynthesisTabProperty = new SimpleBooleanProperty(false);

    stateSpaceProperty.addListener((observable, oldValue, newValue) -> {
      currentOperationProperty.set("none");
      synthesisTypeProperty.set(SynthesisType.ACTION);
      updateCurrentVarNames();
    });
  }


  private void updateCurrentVarNames() {
    final StateSpace stateSpace = getStateSpace();
    if (stateSpace == null) {
      return;
    }
    final ObservableSet<String> variableNames = FXCollections.observableSet();
    final AbstractElement mainComponent = stateSpace.getMainComponent();
    mainComponent.getChildrenOfType(Variable.class)
        .forEach(variable -> variableNames.add(variable.getName()));
    setMachineVarNames(variableNames);
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

  public ObservableSet<String> getMachineVarNames() {
    return machineVarNamesProperty.get();
  }

  public SetProperty<String> machineVarNamesProperty() {
    return machineVarNamesProperty;
  }

  private void setMachineVarNames(final ObservableSet<String> machineVarNamesProperty) {
    Platform.runLater(() -> this.machineVarNamesProperty.set(machineVarNamesProperty));
  }

  public SynthesisType getSynthesisType() {
    return synthesisTypeProperty.get();
  }

  public ObjectProperty<SynthesisType> synthesisTypeProperty() {
    return synthesisTypeProperty;
  }

  public void setSynthesisType(final SynthesisType synthesisType) {
    synthesisTypeProperty.set(synthesisType);
  }

  public String getCurrentOperation() {
    return currentOperationProperty.get();
  }

  public StringProperty currentOperationProperty() {
    return currentOperationProperty;
  }

  public void setCurrentOperation(final String currentOperationProperty) {
    Platform.runLater(() -> this.currentOperationProperty.set(currentOperationProperty));
  }

  public BooleanProperty invariantViolatedProperty() {
    return invariantViolatedProperty;
  }

  public AnimationSelector getAnimationSelector() {
    return animationSelectorProperty.get();
  }

  public StateSpace getStateSpace() {
    return stateSpaceProperty.get();
  }

  public ObjectProperty<StateSpace> stateSpaceProperty() {
    return stateSpaceProperty;
  }

  public void setStateSpace(final StateSpace stateSpace) {
    this.stateSpaceProperty.set(stateSpace);
  }

  public BooleanProperty showLibraryConfigurationProperty() {
    return showLibraryConfigurationProperty;
  }

  public BooleanProperty showSynthesisTabProperty() {
    return showSynthesisTabProperty;
  }
}