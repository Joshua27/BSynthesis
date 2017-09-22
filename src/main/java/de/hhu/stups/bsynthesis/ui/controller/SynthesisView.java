package de.hhu.stups.bsynthesis.ui.controller;

import de.hhu.stups.bsynthesis.prob.GetMachineOperationNamesCommand;
import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.DeadlockRepair;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingProgressIndicator;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingResult;
import de.hhu.stups.bsynthesis.ui.components.SynthesisInfoBox;
import de.hhu.stups.bsynthesis.ui.components.SynthesisProgressIndicator;
import de.prob.statespace.StateSpace;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A {@link ScrollPane} wrapping two {@link Group}. A {@link #contentGroup} displaying the
 * non-scalable content like {@link #synthesisInfoBox}, {@link #modelCheckingIndicator} or
 * {@link #synthesisProgressIndicator}. The {@link #zoomGroup} displays the scalable
 * {@link ValidationPane}.
 */
@Singleton
public class SynthesisView extends ScrollPane implements Initializable {

  private static final double MAX_ZOOM_IN = 1.0;
  private static final double MAX_ZOOM_OUT = 0.3;

  private final SimpleDoubleProperty scaleFactorProperty;
  private final SynthesisContextService synthesisContextService;
  private final UiService uiService;
  private final ModelCheckingService modelCheckingService;

  @FXML
  @SuppressWarnings("unused")
  private Group contentGroup;
  @FXML
  @SuppressWarnings("unused")
  private Group zoomGroup;
  @FXML
  @SuppressWarnings("unused")
  private AnchorPane contentAnchorPane;
  @FXML
  @SuppressWarnings("unused")
  private SynthesisInfoBox synthesisInfoBox;
  @FXML
  @SuppressWarnings("unused")
  private ValidationPane validationPane;
  @FXML
  @SuppressWarnings("unused")
  private ModelCheckingProgressIndicator modelCheckingIndicator;
  @FXML
  @SuppressWarnings("unused")
  private SynthesisProgressIndicator synthesisProgressIndicator;

  /**
   * Initialize scale property and services.
   */
  @Inject
  public SynthesisView(final FXMLLoader loader,
                       final ServiceDelegator serviceDelegator) {
    scaleFactorProperty = new SimpleDoubleProperty(1.0);
    uiService = serviceDelegator.uiService();
    synthesisContextService = serviceDelegator.synthesisContextService();
    modelCheckingService = serviceDelegator.modelCheckingService();

    Loader.loadFxml(loader, this, "synthesis_view.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    contentAnchorPane.getChildren().remove(modelCheckingIndicator);
    contentAnchorPane.getChildren().remove(synthesisProgressIndicator);
    synthesisInfoBox.setTranslateZ(1);

    EasyBind.subscribe(synthesisInfoBox.visibleProperty(), visible -> updateInfoBoxPosition());
    EasyBind.subscribe(hvalueProperty(), number -> {
      updateInfoBoxPosition();
      updateProgressIndicatorPosition(modelCheckingIndicator);
      updateProgressIndicatorPosition(synthesisProgressIndicator);
    });
    EasyBind.subscribe(vvalueProperty(), number -> {
      updateInfoBoxPosition();
      updateProgressIndicatorPosition(modelCheckingIndicator);
      updateProgressIndicatorPosition(synthesisProgressIndicator);
    });
    EasyBind.subscribe(viewportBoundsProperty(), bounds -> {
      contentAnchorPane.setMinSize(bounds.getWidth(), bounds.getHeight());
      updateInfoBoxPosition();
      updateProgressIndicatorPosition(modelCheckingIndicator);
      updateProgressIndicatorPosition(synthesisProgressIndicator);
    });

    initializeUiListener();
    initializeModelCheckingIndicator();
    initializeSynthesisIndicator();
    initializeScaleEvents();
  }

  private void initializeUiListener() {
    uiService.zoomEventStream().subscribe(uiZoom -> {
      final double scaleFactor;
      if (uiZoom.isZoomIn()) {
        scaleFactor = scaleFactorProperty.add(0.1).get();
      } else {
        scaleFactor = scaleFactorProperty.subtract(0.1).get();
      }
      setScaleFactor(Math.round(scaleFactor * 100.0) / 100.0);
    });
    uiService.zoomInEnabledProperty().bind(scaleFactorProperty.lessThan(MAX_ZOOM_IN));
    uiService.zoomOutEnabledProperty().bind(scaleFactorProperty.greaterThan(MAX_ZOOM_OUT));
  }

  private void initializeSynthesisIndicator() {
    updateProgressIndicatorPosition(synthesisProgressIndicator);
    synthesisProgressIndicator.setTranslateZ(1);
    setIndicatorListener(synthesisProgressIndicator,
        synthesisProgressIndicator.indicatorPresentProperty());
  }

  private void initializeModelCheckingIndicator() {
    modelCheckingIndicator.setTranslateZ(1);
    updateProgressIndicatorPosition(modelCheckingIndicator);

    EasyBind.subscribe(modelCheckingIndicator.indicatorPresentProperty(), aBoolean -> {
      if (aBoolean
          && !contentAnchorPane.getChildren().contains(modelCheckingIndicator)) {
        Platform.runLater(() ->
            contentAnchorPane.getChildren().add(modelCheckingIndicator));
      }
      if (!aBoolean) {
        removeProgressIndicator(modelCheckingIndicator);
      }
    });
    EasyBind.subscribe(modelCheckingService.resultProperty(), modelCheckingResult -> {
      if (modelCheckingResult == null) {
        return;
      }
      if (modelCheckingResult.getTrace() == null) {
        synthesisInfoBox.infoTextProperty().set("The model has been checked. No error found.");
        return;
      }
      handleUncoveredError(modelCheckingResult);
    });
    EasyBind.subscribe(modelCheckingService.deadlockRepairProperty(), this::handleDeadlockRepair);
  }

  private void handleUncoveredError(final ModelCheckingResult modelCheckingResult) {
    synthesisContextService.getAnimationSelector().addNewAnimation(modelCheckingResult.getTrace());
    synthesisInfoBox.isMinimizedProperty().set(false);
    synthesisInfoBox.showInfoProperty().set(true);
    if (modelCheckingResult.getUncoveredError().isInvariantViolation()
        && !modelCheckingService.invariantViolationInitialState().get()) {
      // invariant violation, but not in the initial state of the machine which we do not support
      // to resolve using synthesis
      synthesisContextService.setSynthesisType(SynthesisType.GUARD);
      synthesisInfoBox.infoTextProperty().set("Invariant violation found.");
      Platform.runLater(() -> validationPane.initializeNodesFromTrace());
      return;
    }
    synthesisInfoBox.infoTextProperty().set("Deadlock found.");
  }

  /**
   * Two possibilities to repair a deadlock state s.
   * - strengthen the precondition to exclude s from the model
   * - synthesize a new operation or adapt an existing one to transition from s to another state s'
   */
  private void handleDeadlockRepair(
      final DeadlockRepair deadlockRepair) {
    if (deadlockRepair == null) {
      return;
    }
    if (deadlockRepair.isRemoveDeadlock()) {
      synthesisContextService.setSynthesisType(SynthesisType.GUARD);
      Platform.runLater(() -> validationPane.initializeNodesFromTrace());
      return;
    }
    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    synthesisContextService.currentOperationProperty().set(getUniqueDeadlockOpName(0));
    validationPane.initializeDeadlockResolveFromTrace();
  }

  /**
   * Get a unique operation name for resolving a deadlock state, like "repair_deadlock0".
   */
  private String getUniqueDeadlockOpName(final int deadlockOpCount) {
    final String operationName = "repair_deadlock";
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final GetMachineOperationNamesCommand getMachineOperationNamesCommand =
        new GetMachineOperationNamesCommand();
    stateSpace.execute(getMachineOperationNamesCommand);
    if (getMachineOperationNamesCommand.getMachineOperationNames().contains(operationName)) {
      return getUniqueDeadlockOpName(deadlockOpCount + 1);
    }
    return operationName;

  }

  private void initializeScaleEvents() {
    zoomGroup.scaleXProperty().bind(scaleFactorProperty);
    zoomGroup.scaleYProperty().bind(scaleFactorProperty);
    addEventFilter(ScrollEvent.ANY, event -> {
      if (!event.isControlDown()) {
        return;
      }
      final double scaleFactor;
      if (event.getDeltaY() > 0 && (scaleFactorProperty.get() < MAX_ZOOM_IN)) {
        scaleFactor = scaleFactorProperty.add(0.1).get();
      } else if (event.getDeltaY() < 0 && (scaleFactorProperty.get() > MAX_ZOOM_OUT)) {
        scaleFactor = scaleFactorProperty.subtract(0.1).get();
      } else {
        return;
      }
      setScaleFactor(Math.round(scaleFactor * 100.0) / 100.0);
    });
  }

  /**
   * Update the position of {@link #synthesisInfoBox} according to the scroll offset.
   */
  private void updateInfoBoxPosition() {
    final double hmin = getHmin();
    final double viewportWidth = getViewportBounds().getWidth();
    synthesisInfoBox.positionXProperty().set(Math.max(0, ValidationPane.WIDTH - viewportWidth)
        * (getHvalue() - hmin) / (getHmax() - hmin)
        + viewportWidth - synthesisInfoBox.getPrefWidth() - 2);
    // - 2 for a small border to the scroll bar
    final double vmin = getVmin();
    final double viewportHeight = getViewportBounds().getHeight();
    synthesisInfoBox.positionYProperty().set(Math.max(0, ValidationPane.HEIGHT - viewportHeight)
        * (getVvalue() - vmin) / (getVmax() - vmin)
        + viewportHeight - (synthesisInfoBox.isMinimized()
        ? SynthesisInfoBox.MINIMIZED_HEIGHT : SynthesisInfoBox.HEIGHT) - 2);
  }

  /**
   * Update the position of {@link #modelCheckingIndicator} according to the scroll offset.
   */
  private void updateProgressIndicatorPosition(final GridPane progressIndicator) {
    final double hmin = getHmin();
    final double viewportWidth = getViewportBounds().getWidth();
    progressIndicator.setTranslateX(Math.max(0, ValidationPane.WIDTH - viewportWidth)
        * (getHvalue() - hmin) / (getHmax() - hmin)
        + (viewportWidth / 2) - (progressIndicator.getPrefWidth() / 2));
    final double vmin = getVmin();
    final double viewportHeight = getViewportBounds().getHeight();
    progressIndicator.setTranslateY(Math.max(0, ValidationPane.HEIGHT - viewportHeight)
        * (getVvalue() - vmin) / (getVmax() - vmin)
        + (viewportHeight / 2) - (progressIndicator.getPrefHeight() / 2));
  }

  private void removeProgressIndicator(final GridPane progressIndicator) {
    Platform.runLater(() -> contentAnchorPane.getChildren().remove(progressIndicator));
  }

  private boolean isValidZoom(final double scaleFactor) {
    return (scaleFactorProperty.get() > 0 || scaleFactor > 1.0)
        && (scaleFactorProperty.get() < 2.0 || scaleFactor < 1.0);
  }

  @SuppressWarnings("unused")
  private void setScaleFactor(final double scaleFactor) {
    if (isValidZoom(scaleFactor)) {
      Platform.runLater(() -> scaleFactorProperty.set(scaleFactor));
    }
  }

  private void setIndicatorListener(final GridPane indicator,
                                    final BooleanProperty presentProperty) {
    EasyBind.subscribe(presentProperty, present -> {
      if (present
          && !contentAnchorPane.getChildren().contains(indicator)) {
        Platform.runLater(() ->
            contentAnchorPane.getChildren().add(indicator));
      }
      if (!present) {
        removeProgressIndicator(indicator);
      }
    });
  }
}
