package de.hhu.stups.bsynthesis.ui.controller;

import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingProgressIndicator;
import de.hhu.stups.bsynthesis.ui.components.SynthesisInfoBox;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * A {@link ScrollPane} wrapping two {@link Group}. A {@link #contentGroup} displaying the
 * non-scalable content like {@link #synthesisInfoBox} or {@link #modelCheckingIndicator}.
 * The {@link #zoomGroup} displays the scalable {@link ValidationPane}.
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
    synthesisInfoBox.setTranslateZ(1);
    updateInfoBoxPosition();

    hvalueProperty().addListener((observable, oldValue, newValue) -> {
      updateInfoBoxPosition();
      updateProgressIndicatorPosition();
    });
    vvalueProperty().addListener((observable, oldValue, newValue) -> {
      updateInfoBoxPosition();
      updateProgressIndicatorPosition();
    });

    viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
      contentAnchorPane.setMinSize(newValue.getWidth(), newValue.getHeight());
      updateInfoBoxPosition();
      updateProgressIndicatorPosition();
    });

    initializeUiListener();
    initializeModelCheckingIndicator();
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

  private void initializeModelCheckingIndicator() {
    modelCheckingIndicator.setTranslateZ(1);
    updateProgressIndicatorPosition();

    modelCheckingIndicator.indicatorPresentProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue
              && !contentAnchorPane.getChildren().contains(modelCheckingIndicator)) {
            Platform.runLater(() ->
                contentAnchorPane.getChildren().add(modelCheckingIndicator));
          }
          if (!newValue) {
            removeModelCheckingIndicator();
          }
        });
    modelCheckingService.resultProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      if (newValue.getTrace() == null) {
        synthesisContextService.synthesisTypeProperty().set(SynthesisType.ACTION);
        synthesisInfoBox.infoTextProperty().set("No invariant violation found.");
      } else {
        synthesisContextService.getAnimationSelector().addNewAnimation(newValue.getTrace());
        synthesisContextService.setSynthesisType(SynthesisType.GUARD);
        synthesisInfoBox.infoTextProperty().set("Invariant violation found.");
        synthesisInfoBox.isMinimizedProperty().set(false);
        synthesisInfoBox.showInfoProperty().set(true);
        Platform.runLater(() -> validationPane.initializeNodesFromTrace());
      }
    });
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
  private void updateProgressIndicatorPosition() {
    final double hmin = getHmin();
    final double viewportWidth = getViewportBounds().getWidth();
    modelCheckingIndicator.setTranslateX(Math.max(0, ValidationPane.WIDTH - viewportWidth)
        * (getHvalue() - hmin) / (getHmax() - hmin)
        + (viewportWidth / 2) - (modelCheckingIndicator.getPrefWidth() / 2));
    final double vmin = getVmin();
    final double viewportHeight = getViewportBounds().getHeight();
    modelCheckingIndicator.setTranslateY(Math.max(0, ValidationPane.HEIGHT - viewportHeight)
        * (getVvalue() - vmin) / (getVmax() - vmin)
        + (viewportHeight / 2) - (modelCheckingIndicator.getPrefHeight() / 2));
  }
  private void removeModelCheckingIndicator() {
    Platform.runLater(() -> contentAnchorPane.getChildren().remove(modelCheckingIndicator));
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
}
