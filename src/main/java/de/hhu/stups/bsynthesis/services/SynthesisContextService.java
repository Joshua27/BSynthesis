package de.hhu.stups.bsynthesis.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary;
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

import org.reactfx.EventSource;

/**
 * A service providing several properties describing the current synthesis context, i.e. the
 * currently loaded machine's information, results from model checking.
 */
@Singleton
public class SynthesisContextService {


  private final EventSource<ContextEvent> contextEventStream;

  private final SetProperty<String> machineVarNamesProperty;
  private final ObjectProperty<SynthesisType> synthesisTypeProperty;
  private final ObjectProperty<StateSpace> stateSpaceProperty;
  private final StringProperty currentOperationProperty;
  private final BooleanProperty invariantViolatedProperty;
  private final BooleanProperty synthesisSucceededProperty;
  private final ObjectProperty<AnimationSelector> animationSelectorProperty;
  private final ObjectProperty<BLibrary> selectedLibraryComponentsProperty;
  private final StringProperty modifiedMachineCodeProperty;

  /**
   * Initialize all properties and set the injected factories.
   */
  @Inject
  public SynthesisContextService() {
    machineVarNamesProperty = new SimpleSetProperty<>();
    synthesisTypeProperty = new SimpleObjectProperty<>(SynthesisType.NONE);
    stateSpaceProperty = new SimpleObjectProperty<>();
    animationSelectorProperty = new SimpleObjectProperty<>(new AnimationSelector());
    currentOperationProperty = new SimpleStringProperty();
    invariantViolatedProperty = new SimpleBooleanProperty(false);
    selectedLibraryComponentsProperty = new SimpleObjectProperty<>();
    synthesisSucceededProperty = new SimpleBooleanProperty(false);
    modifiedMachineCodeProperty = new SimpleStringProperty();

    contextEventStream = new EventSource<>();

    stateSpaceProperty.addListener((observable, oldValue, newValue) -> {
      synthesisTypeProperty.set(SynthesisType.NONE);
      updateCurrentVarNames();
    });
    contextEventStream.subscribe(contextEvent -> {
      if (ContextEvent.RESET_CONTEXT.equals(contextEvent)) {
        reset();
      }
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

  public ObjectProperty<BLibrary> selectedLibraryComponentsProperty() {
    return selectedLibraryComponentsProperty;
  }

  public BooleanProperty synthesisSucceededProperty() {
    return synthesisSucceededProperty;
  }

  public StringProperty modifiedMachineCodeProperty() {
    return modifiedMachineCodeProperty;
  }

  /**
   * Reset the current synthesis specific properties. For example if a synthesized solution is
   * applied to the model.
   */
  private void reset() {
    synthesisTypeProperty.set(SynthesisType.NONE);
    synthesisSucceededProperty.set(false);
    currentOperationProperty.set(null);
    invariantViolatedProperty.set(false);
  }

  public EventSource<ContextEvent> contextEventStream() {
    return contextEventStream;
  }
}