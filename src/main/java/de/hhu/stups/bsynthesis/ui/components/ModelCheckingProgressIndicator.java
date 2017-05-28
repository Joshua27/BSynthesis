package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.scene.layout.VBox;

@Singleton
public class ModelCheckingProgressIndicator extends VBox implements Initializable {

  private final ModelCheckingService modelCheckingService;
  private final BooleanProperty modelCheckingIndicatorPresentProperty;
  private final StringProperty modelCheckingStatusTextProperty;

  private ValidationPane validationPane;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconCancelModelChecking;
  @FXML
  @SuppressWarnings("unused")
  private Label lbProgress;
  @FXML
  @SuppressWarnings("unused")
  private Label lbProcessedNodes;

  @Inject
  public ModelCheckingProgressIndicator(final FXMLLoader loader,
                                        final ModelCheckingService modelCheckingService) {
    this.modelCheckingService = modelCheckingService;
    modelCheckingIndicatorPresentProperty = new SimpleBooleanProperty(false);
    modelCheckingStatusTextProperty = new SimpleStringProperty("");

    loader.setLocation(getClass().getResource("model_checking_progress_indicator.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (final IOException exception) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE,
          "Loading fxml for the synthesis model checking progress indicator failed.", exception);
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    modelCheckingService.indicatorPresentProperty()
        .bindBidirectional(modelCheckingIndicatorPresentProperty);
    lbProgress.textProperty().bind(
        Bindings.when(Bindings.isNotNull(modelCheckingService.resultProperty()))
            .then(modelCheckingStatusTextProperty)
            .otherwise(""));

    final StringExpression processedNodesBinding =
        Bindings.concat("Processed Nodes: ")
            .concat(modelCheckingService.processedNodesProperty())
            .concat(" / ")
            .concat(modelCheckingService.totalNodesProperty());
    lbProcessedNodes.textProperty().bind(processedNodesBinding);
    lbProcessedNodes.visibleProperty().bind(modelCheckingService.runningProperty()
        .or(modelCheckingService.stateSpaceStatsProperty().isNotNull()));

    iconCancelModelChecking.setOnMouseClicked(event -> {
      modelCheckingService.runningProperty().set(false);
      modelCheckingIndicatorPresentProperty.set(false);
    });

    modelCheckingService.stateSpaceProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        progressIndicator.setVisible(true);
        modelCheckingIndicatorPresentProperty.set(true);
      }
    });

    modelCheckingService.resultProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      progressIndicator.setVisible(false);
      if (newValue.getTrace() != null) {
        modelCheckingStatusTextProperty.set("The model is defective. Error state found.");
      } else {
        modelCheckingStatusTextProperty.set("The model has been checked. No error found.");
      }
    });
  }

  /**
   * Update the position according to the {@link #validationPane validation pane's} scroll offset.
   */
  public void updatePosition() {
    final double hmin = validationPane.getHmin();
    final double viewportWidth = validationPane.getViewportBounds().getWidth();
    setTranslateX(Math.max(0, ValidationPane.WIDTH - viewportWidth)
        * (validationPane.getHvalue() - hmin) / (validationPane.getHmax() - hmin)
        + (viewportWidth / 2) - (getPrefWidth() / 2)); // - 2 for a small border to the scroll bar
    final double vmin = validationPane.getVmin();
    final double viewportHeight = validationPane.getViewportBounds().getHeight();
    setTranslateY(Math.max(0, ValidationPane.HEIGHT - viewportHeight)
        * (validationPane.getVvalue() - vmin) / (validationPane.getVmax() - vmin)
        + (viewportHeight / 2) - (getPrefHeight() / 2));
  }

  public void setValidationPane(final ValidationPane validationPane) {
    this.validationPane = validationPane;
  }

  public BooleanProperty modelCheckingIndicatorPresentProperty() {
    return modelCheckingIndicatorPresentProperty;
  }
}
