package de.hhu.stups.bsynthesis.ui.components.nodes;

import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
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

public class BasicNode extends StackPane {

  private final ValidationPane parent;
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

  BasicNode(final Point2D position,
            final NodeState nodeState,
            final ValidationPane parent,
            final NodeContextMenuFactory nodeContextMenuFactory) {
    this.parent = parent;

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

    contextMenu = nodeContextMenuFactory.create(this);

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
  }

  void refreshBackgroundColor() {
    setStyle("-fx-border-color: #8E8E8E");
    if (transparentBackgroundProperty.get()) {
      setBackground(null);
      return;
    }
    final String notTentativeColor = isInvariantViolated() ? "#FF6464" : "#ABFF98";
    setBackground(new Background(new BackgroundFill(
        Color.web(isTentative() ? "#D8D8D8" : notTentativeColor),
        CornerRadii.EMPTY, Insets.EMPTY)));
  }

  BooleanProperty transparentBackgroundProperty() {
    return transparentBackgroundProperty;
  }

  public void remove() {
    parent.getNodes().remove(this);
  }

  public Double getXPosition() {
    return positionXProperty.get();
  }

  public Double getYPosition() {
    return positionYProperty.get();
  }

  DoubleProperty positionXProperty() {
    return positionXProperty;
  }

  DoubleProperty positionYProperty() {
    return positionYProperty;
  }

  public void setXPosition(final double positionX) {
    positionXProperty.setValue(positionX);
  }

  public void setYPosition(final double positionY) {
    positionYProperty.setValue(positionY);
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

  ValidationPane getValidationPane() {
    return parent;
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