package de.hhu.stups.bsynthesis.ui.components.nodes;

import de.hhu.stups.bsynthesis.services.UiService;
import de.prob.statespace.Trace;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

public class BasicNode extends StackPane {

  public final UiService uiService;

  private final DoubleProperty positionXProperty;
  private final DoubleProperty positionYProperty;
  private final BooleanProperty isExpandedProperty;
  private final ObjectProperty<NodeState> nodeStateProperty;
  private final ObjectProperty<Trace> traceProperty;
  private final NodeContextMenu contextMenu;
  private final DoubleProperty nodeWidthProperty;
  private final DoubleProperty nodeHeightProperty;
  private final BooleanProperty moveIsEnabledProperty;
  private final BooleanProperty transparentBackgroundProperty;
  private final BooleanProperty userValidationProperty;
  private final Timer updateUserValidationTimer;

  BasicNode(final Point2D position,
            final NodeState nodeState,
            final UiService uiService) {
    this.uiService = uiService;

    transparentBackgroundProperty = new SimpleBooleanProperty();
    positionXProperty = new SimpleDoubleProperty(position.getX());
    positionXProperty.addListener((observable, oldValue, newValue) ->
        setLayoutX(newValue.doubleValue()));
    positionYProperty = new SimpleDoubleProperty(position.getY());
    positionYProperty.addListener((observable, oldValue, newValue) ->
        setLayoutY(newValue.doubleValue()));

    nodeWidthProperty = new SimpleDoubleProperty(0);
    nodeHeightProperty = new SimpleDoubleProperty(0);

    isExpandedProperty = new SimpleBooleanProperty(false);
    nodeStateProperty = new SimpleObjectProperty<>(nodeState);
    traceProperty = new SimpleObjectProperty<>();
    moveIsEnabledProperty = new SimpleBooleanProperty(true);

    userValidationProperty = new SimpleBooleanProperty();
    uiService.userValidationEventSource().push(this);

    contextMenu = uiService.getNodeContextMenuFactory().create(this);

    // update the user validation state of the node on move but add a small delay to prevent
    // performance issues
    updateUserValidationTimer = FxTimer.create(java.time.Duration.ofMillis(250), () ->
        uiService.userValidationEventSource().push(this));

    setLayoutX(position.getX());
    setLayoutY(position.getY());
    initializeContextMenuEvent();
    initializeNodeListener();
  }

  private void initializeContextMenuEvent() {
    addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (!event.getButton().equals(MouseButton.SECONDARY)) {
        contextMenu.hide();
        return;
      }
      contextMenu.show(this, event.getScreenX(), event.getScreenY());
      toFront();
      event.consume();
    });
  }

  private void initializeNodeListener() {
    nodeStateProperty().addListener((observable, oldValue, newValue) ->
        refreshBackgroundColor());

    nodeWidthProperty.addListener((observable, oldValue, newValue) -> {
      setMaxSize(nodeWidthProperty.get(), nodeHeightProperty.get());
      setMinSize(nodeWidthProperty.get(), nodeHeightProperty.get());
    });
    nodeHeightProperty.addListener((observable, oldValue, newValue) -> {
      setMaxSize(nodeWidthProperty.get(), nodeHeightProperty.get());
      setMinSize(nodeWidthProperty.get(), nodeHeightProperty.get());
    });

    transparentBackgroundProperty.addListener((observable, oldValue, newValue) ->
        refreshBackgroundColor());
    positionXProperty.addListener((observable, oldValue, newValue) ->
        updateUserValidationTimer.restart());
    positionYProperty.addListener((observable, oldValue, newValue) ->
        updateUserValidationTimer.restart());
  }

  void refreshBackgroundColor() {
    if (transparentBackgroundProperty.get()) {
      setBackground(null);
      return;
    }
    final String notTentativeColor = isInvariantViolated() ? "#FF6464" : "#ABFF98";
    setBackground(new Background(new BackgroundFill(
        Color.web(isTentative() ? "#D8D8D8" : notTentativeColor),
        CornerRadii.EMPTY, Insets.EMPTY)));
  }

  public BooleanProperty userValidationProperty() {
    return userValidationProperty;
  }

  public boolean userValidatedPositive() {
    return userValidationProperty.get();
  }

  public boolean userValidatedNegative() {
    return !userValidationProperty.get();
  }

  BooleanProperty transparentBackgroundProperty() {
    return transparentBackgroundProperty;
  }

  public void remove() {
    uiService.removeNodeEventSource().push(this);
  }

  public Double getXPosition() {
    return positionXProperty.get();
  }

  public void setXPosition(final double positionX) {
    positionXProperty.setValue(positionX);
  }

  public Double getYPosition() {
    return positionYProperty.get();
  }

  public void setYPosition(final double positionY) {
    positionYProperty.setValue(positionY);
  }

  DoubleProperty positionXProperty() {
    return positionXProperty;
  }

  DoubleProperty positionYProperty() {
    return positionYProperty;
  }

  @SuppressWarnings("WeakerAccess")
  boolean isInvariantViolated() {
    return NodeState.INVARIANT_VIOLATED.equals(nodeStateProperty.get());
  }

  public BooleanProperty isExpandedProperty() {
    return isExpandedProperty;
  }

  public BooleanProperty moveIsEnabledProperty() {
    return moveIsEnabledProperty;
  }

  boolean isExpanded() {
    return isExpandedProperty.get();
  }

  @SuppressWarnings("WeakerAccess")
  public Boolean isTentative() {
    return NodeState.TENTATIVE.equals(nodeStateProperty.get());
  }

  public ObjectProperty<NodeState> nodeStateProperty() {
    return nodeStateProperty;
  }

  ObjectProperty<Trace> traceProperty() {
    return traceProperty;
  }

  NodeState getNodeState() {
    return nodeStateProperty.get();
  }

  DoubleProperty nodeWidthProperty() {
    return nodeWidthProperty;
  }

  DoubleProperty nodeHeightProperty() {
    return nodeHeightProperty;
  }
}