package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.ModelCheckingService;
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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

@Singleton
public class ModelCheckingProgressIndicator extends VBox implements Initializable {

  private final ModelCheckingService modelCheckingService;
  private final BooleanProperty indicatorPresentProperty;
  private final StringProperty statusTextProperty;

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

  /**
   * Set {@link #modelCheckingService}, initialize the properties and load fxml resource.
   */
  @Inject
  public ModelCheckingProgressIndicator(final FXMLLoader loader,
                                        final ModelCheckingService modelCheckingService) {
    this.modelCheckingService = modelCheckingService;
    indicatorPresentProperty = new SimpleBooleanProperty(false);
    statusTextProperty = new SimpleStringProperty("");

    Loader.loadFxml(loader, this, "model_checking_progress_indicator.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    modelCheckingService.indicatorPresentProperty()
        .bindBidirectional(indicatorPresentProperty);
    lbProgress.textProperty().bind(
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

    iconCancelModelChecking.setOnMouseClicked(event -> {
      modelCheckingService.runningProperty().set(false);
      indicatorPresentProperty.set(false);
    });

    modelCheckingService.stateSpaceEventStream().subscribe(stateSpace -> {
      if (stateSpace != null) {
        progressIndicator.setVisible(true);
        indicatorPresentProperty.set(true);
      }
    });

    modelCheckingService.resultProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        return;
      }
      progressIndicator.setVisible(false);
      if (newValue.getTrace() != null) {
        statusTextProperty.set("The model is defective. Error state found.");
      } else {
        statusTextProperty.set("The model has been checked. No error found.");
      }
    });
  }

  public BooleanProperty indicatorPresentProperty() {
    return indicatorPresentProperty;
  }
}
