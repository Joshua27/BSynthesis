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
 * A service providing several properties describing the current synthesis context, e.g., the
 * currently loaded machine's information or the synthesis type.
 */
@Singleton
public class SynthesisContextService {

  private final EventSource<ContextEvent> contextEventStream;
  private final SetProperty<String> machineVarNamesProperty;
  private final ObjectProperty<SynthesisType> synthesisTypeProperty;
  private final ObjectProperty<SpecificationType> specificationTypeProperty;
  private final ObjectProperty<StateSpace> stateSpaceProperty;
  private final ObjectProperty<AnimationSelector> animationSelectorProperty;
  private final ObjectProperty<BLibrary> selectedLibraryComponentsProperty;
  private final ObjectProperty<SolverBackend> solverBackendProperty;
  private final StringProperty currentOperationProperty;
  private final StringProperty modifiedMachineCodeProperty;
  private final BooleanProperty invariantViolatedProperty;
  private final BooleanProperty synthesisSucceededProperty;
  private final BooleanProperty synthesisRunningProperty;
  private final BooleanProperty synthesisSuspendedProperty;
  private final BooleanProperty useDefaultLibraryProperty;
  private final BooleanProperty modifyInvariantsProperty;
  private StringProperty behaviorSatisfiedProperty;

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
    useDefaultLibraryProperty = new SimpleBooleanProperty();
    selectedLibraryComponentsProperty = new SimpleObjectProperty<>();
    synthesisSucceededProperty = new SimpleBooleanProperty(false);
    synthesisSuspendedProperty = new SimpleBooleanProperty(false);
    synthesisRunningProperty = new SimpleBooleanProperty(false);
    modifiedMachineCodeProperty = new SimpleStringProperty();
    behaviorSatisfiedProperty = new SimpleStringProperty();
    solverBackendProperty = new SimpleObjectProperty<>(SolverBackend.PROB);
    specificationTypeProperty = new SimpleObjectProperty<>(SpecificationType.CLASSICAL_B);
    modifyInvariantsProperty = new SimpleBooleanProperty(false);

    contextEventStream = new EventSource<>();

    stateSpaceProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      synthesisTypeProperty.set(SynthesisType.NONE);
      final ObservableSet<String> variableNames = FXCollections.observableSet();
      final AbstractElement mainComponent = newValue.getMainComponent();
      mainComponent.getChildrenOfType(Variable.class)
          .forEach(variable -> variableNames.add(variable.getName()));
      setMachineVarNames(variableNames);
    });
    contextEventStream.subscribe(contextEvent -> {
      if (ContextEvent.RESET_CONTEXT.equals(contextEvent)) {
        reset();
      }
    });
  }

  public ObservableSet<String> getMachineVarNames() {
    return machineVarNamesProperty.get();
  }

  private void setMachineVarNames(final ObservableSet<String> machineVarNamesProperty) {
    Platform.runLater(() -> this.machineVarNamesProperty.set(machineVarNamesProperty));
  }

  public SetProperty<String> machineVarNamesProperty() {
    return machineVarNamesProperty;
  }

  public SynthesisType getSynthesisType() {
    return synthesisTypeProperty.get();
  }

  public void setSynthesisType(final SynthesisType synthesisType) {
    Platform.runLater(() -> synthesisTypeProperty.set(synthesisType));
  }

  public ObjectProperty<SynthesisType> synthesisTypeProperty() {
    return synthesisTypeProperty;
  }

  public String getCurrentOperation() {
    return currentOperationProperty.get();
  }

  public void setCurrentOperation(final String currentOperation) {
    Platform.runLater(() -> this.currentOperationProperty.set(currentOperation));
  }

  public SpecificationType getSpecificationType() {
    return specificationTypeProperty.get();
  }

  public void setSpecificationType(final SpecificationType specificationType) {
    specificationTypeProperty.set(specificationType);
  }

  public StringProperty currentOperationProperty() {
    return currentOperationProperty;
  }

  public BooleanProperty invariantViolatedProperty() {
    return invariantViolatedProperty;
  }

  public BooleanProperty useDefaultLibraryProperty() {
    return useDefaultLibraryProperty;
  }

  public AnimationSelector getAnimationSelector() {
    return animationSelectorProperty.get();
  }

  public StateSpace getStateSpace() {
    return stateSpaceProperty.get();
  }

  public void setStateSpace(final StateSpace stateSpace) {
    this.stateSpaceProperty.set(stateSpace);
  }

  public ObjectProperty<StateSpace> stateSpaceProperty() {
    return stateSpaceProperty;
  }

  public ObjectProperty<BLibrary> selectedLibraryComponentsProperty() {
    return selectedLibraryComponentsProperty;
  }

  public ObjectProperty<SolverBackend> solverBackendProperty() {
    return solverBackendProperty;
  }

  public BooleanProperty synthesisSucceededProperty() {
    return synthesisSucceededProperty;
  }

  public BooleanProperty synthesisRunningProperty() {
    return synthesisRunningProperty;
  }

  public BooleanProperty synthesisSuspendedProperty() {
    return synthesisSuspendedProperty;
  }

  public StringProperty modifiedMachineCodeProperty() {
    return modifiedMachineCodeProperty;
  }

  /**
   * Reset the current synthesis specific properties. For example if a synthesized solution is
   * applied to the model.
   */
  public void reset() {
    Platform.runLater(() -> {
      synthesisTypeProperty.set(SynthesisType.NONE);
      currentOperationProperty.set(null);
    });
    synthesisSucceededProperty.set(false);
    invariantViolatedProperty.set(false);
    modifyInvariantsProperty.set(false);
    selectedLibraryComponentsProperty.get().defaultLibraryExpansionProperty().set(1);
  }

  public EventSource<ContextEvent> contextEventStream() {
    return contextEventStream;
  }

  public boolean useDefaultLibrary() {
    return useDefaultLibraryProperty().get();
  }

  public BLibrary getSelectedLibraryComponents() {
    return selectedLibraryComponentsProperty.get();
  }

  public BooleanProperty modifyInvariantsProperty() {
    return modifyInvariantsProperty;
  }

  public StringProperty behaviorSatisfiedProperty() {
    return behaviorSatisfiedProperty;
  }
}