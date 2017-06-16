package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.Trace;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

@Singleton
public class SynthesisInfoBox extends VBox implements Initializable {

  private static final double HEIGHT = 150.0;
  private static final double MINIMIZED_HEIGHT = 40.0;
  private static final double DIFFERENCE_HEIGHT = HEIGHT - MINIMIZED_HEIGHT;

  private final SynthesisContextService synthesisContextService;
  private final BooleanProperty isMinimizedProperty;
  private final BooleanProperty showInfoProperty;
  private final DoubleProperty positionXProperty;
  private final DoubleProperty positionYProperty;
  private final StringProperty infoTextProperty;
  private final Timeline timeline;

  private ValidationPane validationPane;

  @FXML
  @SuppressWarnings("unused")
  private Label lbCurrentOperation;
  @FXML
  @SuppressWarnings("unused")
  private Label lbSynthesisType;
  @FXML
  @SuppressWarnings("unused")
  private Label lbInfo;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconShowOrHide;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconClose;

  /**
   * Initialize the properties and load the fxml resource.
   */
  @Inject
  public SynthesisInfoBox(final FXMLLoader loader,
                          final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;

    isMinimizedProperty = new SimpleBooleanProperty(false);
    showInfoProperty = new SimpleBooleanProperty(false);
    positionXProperty = new SimpleDoubleProperty();
    positionYProperty = new SimpleDoubleProperty();
    infoTextProperty = new SimpleStringProperty();
    timeline = new Timeline();

    Loader.loadFxml(loader, this, "synthesis_info_box.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    setPrefHeight(HEIGHT);

    synthesisContextService.stateSpaceProperty().addListener((observable, oldValue, newValue) -> {
      showInfoProperty.set(newValue != null);
      infoTextProperty.set("");
    });

    showInfoProperty.addListener((observable, oldValue, newValue) ->
        isMinimizedProperty().set(!newValue));

    visibleProperty().bind(showInfoProperty
        .and(synthesisContextService.stateSpaceProperty().isNotNull())
        .and(synthesisContextService.synthesisTypeProperty().isNotEqualTo(SynthesisType.NONE)));

    iconShowOrHide.glyphNameProperty().bind(
        Bindings.when(isMinimizedProperty()).then("CHEVRON_UP").otherwise("CHEVRON_DOWN"));
    iconShowOrHide.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (MouseButton.SECONDARY.equals(event.getButton())) {
        return;
      }
      isMinimizedProperty().set(!isMinimized());
    });

    iconClose.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (MouseButton.SECONDARY.equals(event.getButton())) {
        return;
      }
      showInfoProperty().set(false);
    });

    lbInfo.textProperty().bind(infoTextProperty);

    positionXProperty.addListener((observable, oldValue, newValue) ->
        setLayoutX(newValue.doubleValue()));
    positionYProperty.addListener((observable, oldValue, newValue) ->
        setLayoutY(newValue.doubleValue()));

    isMinimizedProperty.addListener((observable, oldValue, newValue) -> showOrHide(newValue));

    lbCurrentOperation.textProperty().bind(Bindings.createStringBinding(() ->
            "Current operation: "
                + ((synthesisContextService.getCurrentOperation() != null)
                ? synthesisContextService.getCurrentOperation() : "none"),
        synthesisContextService.currentOperationProperty()));

    synthesisContextService.synthesisTypeProperty().addListener(
        (observable, oldValue, newValue) ->
            Platform.runLater(() -> lbSynthesisType.setText("Synthesis type: "
                + synthesisContextService.getSynthesisType().toString())));

    synthesisContextService.currentOperationProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue == null) {
            infoTextProperty.set("");
          }
        });
  }

  /**
   * Update the position according to the {@link #validationPane validation pane's} scroll offset.
   */
  public void updatePosition() {
    final double hmin = validationPane.getHmin();
    final double viewportWidth = validationPane.getViewportBounds().getWidth();
    positionXProperty.set(Math.max(0, ValidationPane.WIDTH - viewportWidth)
        * (validationPane.getHvalue() - hmin) / (validationPane.getHmax() - hmin)
        + viewportWidth - getPrefWidth() - 2); // - 2 for a small border to the scroll bar
    final double vmin = validationPane.getVmin();
    final double viewportHeight = validationPane.getViewportBounds().getHeight();
    positionYProperty.set(Math.max(0, ValidationPane.HEIGHT - viewportHeight)
        * (validationPane.getVvalue() - vmin) / (validationPane.getVmax() - vmin)
        + viewportHeight - (isMinimized() ? MINIMIZED_HEIGHT : HEIGHT) - 2);
  }

  /**
   * Timeline animation to minimize or maximize the box. Add info labels on finished but remove the
   * labels directly when animation is minimizing.
   */
  private void showOrHide(final boolean minimize) {
    timeline.getKeyFrames().clear();
    final double targetHeight = minimize ? MINIMIZED_HEIGHT : HEIGHT;
    final double targetYPosition = positionYProperty.get() + (minimize ? DIFFERENCE_HEIGHT
        : -DIFFERENCE_HEIGHT);
    final KeyFrame expandAnimation = new KeyFrame(Duration.millis(250),
        new KeyValue(positionYProperty, targetYPosition),
        new KeyValue(prefHeightProperty(), targetHeight));
    timeline.getKeyFrames().add(expandAnimation);
    Platform.runLater(timeline::play);

    timeline.setOnFinished(event -> {
      if (!minimize) {
        getChildren().add(lbCurrentOperation);
        getChildren().add(lbSynthesisType);
        getChildren().add(lbInfo);
      }
    });
    if (minimize) {
      getChildren().remove(lbCurrentOperation);
      getChildren().remove(lbSynthesisType);
      getChildren().remove(lbInfo);
    }
  }

  public void setValidationPane(final ValidationPane validationPane) {
    this.validationPane = validationPane;
  }

  public StringProperty infoTextProperty() {
    return infoTextProperty;
  }

  public BooleanProperty isMinimizedProperty() {
    return isMinimizedProperty;
  }

  private boolean isMinimized() {
    return isMinimizedProperty.get();
  }

  public BooleanProperty showInfoProperty() {
    return showInfoProperty;
  }

  /**
   * Set the state from a given {@link Trace} from the model checker.
   */
  @SuppressWarnings("unused")
  public void setStateFromTrace(final Trace trace) {
    if (trace == null) {
      Platform.runLater(() -> {
        synthesisContextService.synthesisTypeProperty().set(SynthesisType.ACTION);
        infoTextProperty.set("No invariant violation found.");
      });
    } else {
      Platform.runLater(() -> {
        synthesisContextService.getAnimationSelector().addNewAnimation(trace);
        synthesisContextService.setSynthesisType(SynthesisType.GUARD);
        infoTextProperty.set("Invariant violation found.");
        isMinimizedProperty.set(false);
        showInfoProperty.set(true);
      });
    }
  }

  void reset() {
    Platform.runLater(() -> infoTextProperty.set(""));
  }
}