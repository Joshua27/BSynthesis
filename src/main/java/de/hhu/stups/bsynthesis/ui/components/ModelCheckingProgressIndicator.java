package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.ResourceBundle;

@Singleton
public class ModelCheckingProgressIndicator extends GridPane implements Initializable {

  private final ModelCheckingService modelCheckingService;
  private final BooleanProperty indicatorPresentProperty;
  private final StringProperty statusTextProperty;
  private final SynthesisContextService synthesisContextService;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconCancelModelChecking;
  @FXML
  @SuppressWarnings("unused")
  private Label lbStatus;
  @FXML
  @SuppressWarnings("unused")
  private Label lbProcessedNodes;
  @FXML
  @SuppressWarnings("unused")
  private HBox boxDeadlock;

  /**
   * Set {@link #modelCheckingService}, initialize the properties and load fxml resource.
   */
  @Inject
  public ModelCheckingProgressIndicator(final FXMLLoader loader,
                                        final ServiceDelegator serviceDelegator) {
    this.modelCheckingService = serviceDelegator.modelCheckingService();
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    indicatorPresentProperty = new SimpleBooleanProperty(false);
    statusTextProperty = new SimpleStringProperty("");

    Loader.loadFxml(loader, this, "model_checking_progress_indicator.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    getChildren().remove(boxDeadlock);

    iconCancelModelChecking.setOnMouseClicked(event -> quitModelChecking());

    initializeStatusBindings();
    initializeServiceBindings();
  }

  private void initializeStatusBindings() {
    lbStatus.textProperty().bind(
        Bindings.when(Bindings.isNotNull(modelCheckingService.resultProperty()))
            .then(statusTextProperty)
            .otherwise(""));
    final StringExpression processedNodesBinding =
        Bindings.concat("Processed Nodes: ")
            .concat(modelCheckingService.processedNodesProperty())
            .concat(" / ")
            .concat(modelCheckingService.totalNodesProperty());
    lbProcessedNodes.textProperty().bind(processedNodesBinding);
    lbProcessedNodes.visibleProperty().bind(modelCheckingService.runningProperty()
        .or(modelCheckingService.stateSpaceStatsProperty().isNotNull()));
  }

  private void initializeServiceBindings() {
    modelCheckingService.indicatorPresentProperty()
        .bindBidirectional(indicatorPresentProperty);
    EasyBind.subscribe(indicatorPresentProperty, isPresent -> {
      if (isPresent) {
        iconCancelModelChecking.setVisible(true);
      }
    });
    modelCheckingService.stateSpaceEventStream().subscribe(stateSpace -> {
      if (stateSpace != null) {
        progressIndicator.setVisible(true);
        indicatorPresentProperty.set(true);
      }
    });
    EasyBind.subscribe(modelCheckingService.resultProperty(), modelCheckingResult -> {
      if (modelCheckingResult == null) {
        return;
      }
      progressIndicator.setVisible(false);
      if (modelCheckingResult.getTrace() != null) {
        handleUncoveredError(modelCheckingResult);
      } else {
        statusTextProperty.set("The model has been checked. No error found.");
      }
    });
  }

  // TODO: disable menu if mc indicator is present or handle iconCancelModelChecking visibility

  private void handleUncoveredError(final ModelCheckingResult modelCheckingResult) {
    final String affectedOperation =
        modelCheckingResult.getTrace().getCurrentTransition().getName();
    if (modelCheckingResult.getUncoveredError().isInvariantViolation()) {
      synthesisContextService.setCurrentOperation(affectedOperation);
      statusTextProperty.set("Invariant violation found.");
      return;
    }
    if ("$initialise_machine".equals(affectedOperation)) {
      // deadlock in initialization state: the only thing we can do is to synthesize a new operation
      synthesisContextService.setCurrentOperation(null);
      statusTextProperty.set("Deadlock found in initialization state.");
      modelCheckingService.deadlockRepairProperty().set(DeadlockRepair.RESOLVE_DEADLOCK);
      return;
    }
    synthesisContextService.setCurrentOperation(affectedOperation);
    statusTextProperty.set("Deadlock found.");
    iconCancelModelChecking.setVisible(false);
    getChildren().add(boxDeadlock);
  }

  /**
   * Remove the deadlock state by strengthening the precondition of the affected operation.
   */
  @FXML
  @SuppressWarnings("unused")
  public void removeDeadlockState() {
    quitModelChecking();
    iconCancelModelChecking.setVisible(true);
    getChildren().remove(boxDeadlock);
    modelCheckingService.deadlockRepairProperty().set(DeadlockRepair.REMOVE_DEADLOCK);
  }

  /**
   * Keep the deadlock state but synthesize a new operation or adapt an existing one to
   * transition from the deadlock state s to a new state s' to resolve the deadlock.
   */
  @FXML
  @SuppressWarnings("unused")
  public void resolveDeadlockState() {
    iconCancelModelChecking.setVisible(true);
    getChildren().remove(boxDeadlock);
    quitModelChecking();
    modelCheckingService.deadlockRepairProperty().set(DeadlockRepair.RESOLVE_DEADLOCK);
  }

  private void quitModelChecking() {
    modelCheckingService.runningProperty().set(false);
    indicatorPresentProperty.set(false);
  }

  public BooleanProperty indicatorPresentProperty() {
    return indicatorPresentProperty;
  }
}
