package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingProgressIndicator;
import de.hhu.stups.bsynthesis.ui.components.NodesFromTracePositionGenerator;
import de.hhu.stups.bsynthesis.ui.components.SynthesisInfoBox;
import de.hhu.stups.bsynthesis.ui.components.ValidationContextMenu;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeLine;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.prob.statespace.State;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.factories.ValidationContextMenuFactory;
import de.prob.statespace.Trace;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import static java.beans.Beans.isInstanceOf;

/**
 * The validation pane which is graphically split in two areas where the left side contains valid
 * nodes and the right side invalid ones.
 */
@Singleton
public class ValidationPane extends ScrollPane implements Initializable {

  public static final double WIDTH = 2700.0;
  public static final double HEIGHT = 1600.0;

  private static final double SCALE_FACTOR = 1.1;
  private static final double MAX_ZOOM_IN = 1.0;
  private static final double MAX_ZOOM_OUT = 0.0;
  private static final int MODEL_CHECKING_STATE_AMOUNT = 5;

  private final ObservableList<BasicNode> nodes;

  private final SimpleDoubleProperty scaleFactorProperty;
  private final StateNodeFactory stateNodeFactory;
  private final ValidationContextMenu validationContextMenu;
  private final SynthesisContextService synthesisContextService;
  private final ModelCheckingService modelCheckingService;
  private final ModelCheckingProgressIndicator modelCheckingProgressIndicator;

  private BasicNode dragNode;
  private double offsetX;
  private double offsetY;

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
  private Pane contentPane;
  @FXML
  @SuppressWarnings("unused")
  private SynthesisInfoBox synthesisInfoBox;

  @Inject
  public ValidationPane(final FXMLLoader loader,
                        final StateNodeFactory stateNodeFactory,
                        final ValidationContextMenuFactory validationContextMenuFactory,
                        final ModelCheckingProgressIndicator modelCheckingProgressIndicator,
                        final ModelCheckingService modelCheckingService,
                        final SynthesisContextService synthesisContextService) {
    this.stateNodeFactory = stateNodeFactory;
    this.synthesisContextService = synthesisContextService;
    this.modelCheckingService = modelCheckingService;
    this.modelCheckingProgressIndicator = modelCheckingProgressIndicator;

    scaleFactorProperty = new SimpleDoubleProperty(1.0);
    validationContextMenu = validationContextMenuFactory.create(SynthesisType.ACTION);
    nodes = FXCollections.observableArrayList();

    loader.setLocation(getClass().getResource("validation_pane.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (final IOException exception) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE, "Loading fxml for the synthesis main view failed.", exception);
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    synthesisInfoBox.setValidationPane(this);
    validationContextMenu.setValidationPane(this);

    contentPane.requestFocus();
    contentPane.setPrefSize(WIDTH, HEIGHT);

    nodes.addListener((ListChangeListener<BasicNode>) change -> {
      /*  add nodes to the pane when added to {@link #nodes} */
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(node -> Platform.runLater(() ->
              contentPane.getChildren().add(node)));
        } else if (change.wasRemoved()) {
          change.getRemoved().forEach(node -> Platform.runLater(() ->
              removeNode(node)));
        }
      }
    });

    synthesisContextService.stateSpaceProperty().addListener((observable, oldValue, newValue) -> {
      getNodes().clear();
      modelCheckingService.indicatorPresentProperty().set(false);
    });

    synthesisInfoBox.setTranslateZ(1);
    synthesisInfoBox.updatePosition();

    hvalueProperty().addListener((observable, oldValue, newValue) -> {
      synthesisInfoBox.updatePosition();
      modelCheckingProgressIndicator.updatePosition();
    });
    vvalueProperty().addListener((observable, oldValue, newValue) -> {
      synthesisInfoBox.updatePosition();
      modelCheckingProgressIndicator.updatePosition();
    });

    viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
      contentAnchorPane.setMinSize(newValue.getWidth(), newValue.getHeight());
      synthesisInfoBox.updatePosition();
      modelCheckingProgressIndicator.updatePosition();
    });
    synthesisContextService.synthesisTypeProperty().set(SynthesisType.ACTION);
    initializeModelCheckingIndicator();
    initializeContextMenu();
    initializeScaleEvents();
    initializeDragEvents();
    initializeBackground();
  }

  /**
   * Remove a {@link BasicNode} from the pane, delete the node from all ancestors and remove edges
   * with this node.
   */
  @SuppressWarnings("unused")
  private void removeNode(final BasicNode nodeToRemove) {
    Platform.runLater(() -> contentPane.getChildren().remove(nodeToRemove));
    contentPane.getChildren().forEach(node -> {
      if (node instanceof NodeLine && (nodeToRemove.equals(((NodeLine) node).getSource())
          || nodeToRemove.equals(((NodeLine) node).getTarget()))) {
        Platform.runLater(() -> contentPane.getChildren().remove(node));
      }
    });
    if (isInstanceOf(nodeToRemove, StateNode.class)) {
      nodes.forEach(node -> {
        final StateNode stateNode = (StateNode) node;
        if (stateNode.successorProperty().contains(nodeToRemove)) {
          stateNode.successorProperty().remove(nodeToRemove);
        } else if (stateNode.predecessorProperty().contains(nodeToRemove)) {
          stateNode.predecessorProperty().remove(nodeToRemove);
        }
      });
    }
  }

  private void initializeContextMenu() {
    contentPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (isValidDragObject((Node) event.getTarget())   // hide/don't show when a node is clicked
          || !event.getButton().equals(MouseButton.SECONDARY)
          || isInstanceOf(event.getTarget(), TransitionNode.class)
          || synthesisContextService.getStateSpace() == null) {
        validationContextMenu.hide();
        return;
      }
      validationContextMenu.getGraphicPositionProperty()
          .setValue(new Point2D(event.getX(), event.getY()));
      validationContextMenu.show(this, event.getScreenX(), event.getScreenY());
    });
  }

  private void initializeModelCheckingIndicator() {
    modelCheckingProgressIndicator.setValidationPane(this);
    modelCheckingProgressIndicator.setTranslateZ(1);
    modelCheckingProgressIndicator.updatePosition();

    modelCheckingProgressIndicator.modelCheckingIndicatorPresentProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue
              && !contentAnchorPane.getChildren().contains(modelCheckingProgressIndicator)) {
            Platform.runLater(() ->
                contentAnchorPane.getChildren().add(modelCheckingProgressIndicator));
          }
          if (!newValue) {
            removeModelCheckingIndicator();
          }
        });
    modelCheckingService.stateSpaceProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
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
        synthesisContextService.setSynthesisType(SynthesisType.GUARD_OR_INVARIANT);
        synthesisInfoBox.infoTextProperty().set("Invariant violation found.");
        synthesisInfoBox.isMinimizedProperty().set(false);
        synthesisInfoBox.showInfoProperty().set(true);
        Platform.runLater(this::initializeNodesFromTrace);
      }
    });
  }

  private void removeModelCheckingIndicator() {
    Platform.runLater(() -> contentAnchorPane.getChildren().remove(modelCheckingProgressIndicator));
  }

  private void initializeScaleEvents() {
    scaleFactorProperty.addListener((observable, oldValue, newValue) -> {
      if (!isValidZoom(newValue.doubleValue())) {
        return;
      }
      if ((newValue.doubleValue() - 0.0001 < 1.00001 && newValue.doubleValue() - 0.0001 > 0.99999)
          || oldValue.equals(newValue)) {
        contentPane.setPrefSize(WIDTH, HEIGHT);
        return;
      }
      final double scaleFactor = (oldValue.doubleValue() > newValue.doubleValue())
          ? 1 / SCALE_FACTOR : SCALE_FACTOR;
      zoomGroup.setScaleX(zoomGroup.getScaleX() * scaleFactor);
      zoomGroup.setScaleY(zoomGroup.getScaleY() * scaleFactor);
    });

    addEventFilter(ScrollEvent.ANY, event -> {
      if (!event.isControlDown()) {
        return;
      }
      if (event.getDeltaY() > 0 && (scaleFactorProperty.get() <= MAX_ZOOM_IN)) {
        setScaleFactor(Math.round(scaleFactorProperty.add(0.1).get() * 100.0) / 100.0);
      } else if (event.getDeltaY() < 0 && (scaleFactorProperty.get() > MAX_ZOOM_OUT)) {
        setScaleFactor(Math.round(scaleFactorProperty.subtract(0.1).get() * 100.0) / 100.0);
      }
    });
  }

  private void initializeDragEvents() {
    contentPane.setOnMousePressed(event -> {
      if (dragNode != null || event.getButton().equals(MouseButton.SECONDARY)) {
        return;
      }
      Node node = (Node) event.getTarget();
      if (isValidDragObject(node)) {
        while (node.getParent() != contentPane) {
          node = node.getParent();
        }
        offsetX = node.getLayoutX() - event.getX();
        offsetY = node.getLayoutY() - event.getY();
        dragNode = (BasicNode) node;
        dragNode.toFront();
        childrenToFrontIfNecessary(dragNode);
      }
    });

    contentPane.setOnMouseReleased(event -> dragNode = null);

    contentPane.setOnMouseDragged(event -> {
      if (dragNode == null) {
        return;
      }
      final double newXPosition = event.getX() + offsetX;
      final double newYPosition = event.getY() + offsetY;
      if (isValidXPosition(newXPosition, dragNode.getWidth())) {
        dragNode.setXPosition(newXPosition);
      }
      if (isValidYPosition(newYPosition, dragNode.getHeight())) {
        dragNode.setYPosition(newYPosition);
      }
    });
  }

  private void childrenToFrontIfNecessary(final BasicNode dragNode) {
    if (dragNode instanceof TransitionNode) {
      ((TransitionNode) dragNode).childrenToFront();
    }
  }

  private void initializeBackground() {
    final double halfWidth = WIDTH / 2;

    final Rectangle validRectangle;
    validRectangle = new Rectangle(0, 0, halfWidth, HEIGHT);
    validRectangle.setFill(Color.web("#DFFFD8"));
    contentPane.getChildren().add(0, validRectangle);

    final Rectangle invalidRectangle;
    invalidRectangle = new Rectangle(halfWidth, 0, halfWidth, HEIGHT);
    invalidRectangle.setFill(Color.web("#FFD8F0"));
    contentPane.getChildren().add(1, invalidRectangle);

    final Line splitLine;
    splitLine = new Line(halfWidth, 0.0, halfWidth, HEIGHT);
    splitLine.setStyle("-fx-stroke-dash-array: 0.1 5.0;");
    contentPane.getChildren().add(2, splitLine);
  }

  /**
   * Initialize the nodes from the trace derived by the model checker in case the model is
   * defective. Determine the machine operation that leads into the erroneous state.
   */
  public void initializeNodesFromTrace() {
    final Trace initialMcTrace = modelCheckingService.errorFoundProperty().get();
    if (initialMcTrace == null) {
      return;
    }
    synthesisContextService.getAnimationSelector().changeCurrentAnimation(initialMcTrace);
    synthesisContextService.stateSpaceProperty().set(initialMcTrace.getStateSpace());
    boolean invariantViolatingOpIsSet = false;
    final NodesFromTracePositionGenerator nodesFromTraceGenerator =
        new NodesFromTracePositionGenerator();

    for (int i = 0; i < MODEL_CHECKING_STATE_AMOUNT; i++) {
      final Trace trace = synthesisContextService.getAnimationSelector().getCurrentTrace();
      if (SynthesisType.ACTION.equals(synthesisContextService.getSynthesisType())
          || trace == null) {
        return;
      }
      final State currentState = trace.getCurrentState();
      final StateNode stateNode = stateNodeFactory.create(currentState, trace,
          currentState.isInvariantOk()
              ? nodesFromTraceGenerator.getNextValidNodePosition()
              : nodesFromTraceGenerator.getNextInvalidNodePosition(),
          getNodeState(currentState));

      final StateNode previousNode = nodesFromTraceGenerator.getPreviousNode();
      final Trace previousTrace = nodesFromTraceGenerator.getPreviousTrace();
      if (previousNode != null) {
        stateNode.successorProperty().add(previousNode);
        previousNode.predecessorProperty().add(stateNode);
      }
      // find operation that violates the invariant when following the trace
      if (!invariantViolatingOpIsSet && previousNode != null
          && !previousNode.getState().isInvariantOk() && stateNode.getState().isInvariantOk()) {
        synthesisContextService.setCurrentOperation(previousTrace.getCurrentTransition().getName());
        invariantViolatingOpIsSet = true;
      }
      addNode(stateNode);
      if (!trace.canGoBack()) {
        return;
      }
      nodesFromTraceGenerator.setPreviousNode(stateNode);
      nodesFromTraceGenerator.setPreviousTrace(trace);
      synthesisContextService.getAnimationSelector().changeCurrentAnimation(trace.back());
    }
  }

  private boolean isValidZoom(final double scaleFactor) {
    return (scaleFactorProperty.get() > 0 || scaleFactor > 1.0)
        && (scaleFactorProperty.get() < 2.0 || scaleFactor < 1.0);
  }

  public boolean isValidXPosition(final double xPosition, final double nodeWidth) {
    return xPosition > 5.0 && xPosition + nodeWidth < WIDTH - 5.0;
  }

  public boolean isValidYPosition(final double yPosition, final double nodeHeight) {
    return yPosition > 5.0 && yPosition + nodeHeight < HEIGHT - 5.0;
  }

  private boolean isValidDragObject(final Node node) {
    return node != null && (isInstanceOf(node, BasicNode.class)
        && ((BasicNode) node).moveIsEnabledProperty().get()
        || isValidDragObject(node.getParent()));
  }

  /**
   * Return all nodes whose center is on the green side of the pane, i.e. in the left half.
   */
  @SuppressWarnings("unused")
  private List<Node> getValidNodes() {
    return nodes.stream()
        .filter(node -> node.getTranslateX() + node.getWidth() / 2 < ValidationPane.WIDTH / 2)
        .collect(Collectors.toList());
  }

  /**
   * Return all nodes whose center is on the red side of the pane, i.e. in the right half.
   */
  @SuppressWarnings("unused")
  private List<Node> getInvalidNodes() {
    return nodes.stream()
        .filter(node -> node.getTranslateX() + node.getWidth() / 2 >= ValidationPane.WIDTH / 2)
        .collect(Collectors.toList());
  }

  public ObservableList<BasicNode> getNodes() {
    return nodes;
  }

  @SuppressWarnings("unused")
  private void setScaleFactor(final double scaleFactor) {
    Platform.runLater(() -> scaleFactorProperty.set(scaleFactor));
  }

  /**
   * When adding a {@link StateNode} to the pane check if ancestors exist and set the corresponding
   * relations.
   */
  public void addNode(final BasicNode node) {
    Platform.runLater(() -> nodes.add(node));
    if (node instanceof StateNode) {
      addStateNode((StateNode) node);
    }
  }

  /**
   * After adding the {@link StateNode} itself we set the corresponding connections to
   * its successor and predecessor nodes if present.
   */
  private void addStateNode(final StateNode stateNode) {
    final StateNode successorNode = stateNode.getSuccessorFromTrace();
    if (successorNode != null) {
      final StateNode nodeExists = containsStateNode(successorNode);
      if (nodeExists != null) {
        nodeExists.predecessorProperty().add(stateNode);
        stateNode.successorProperty().add(nodeExists);
      }
    }
    final StateNode predecessorNode = stateNode.getPredecessorFromTrace();
    if (predecessorNode != null) {
      final StateNode nodeExists = containsStateNode(predecessorNode);
      if (nodeExists != null) {
        nodeExists.successorProperty().add(stateNode);
        stateNode.predecessorProperty().add(nodeExists);
      }
    }
  }

  public void addNodeConnection(final NodeLine nodeConnection) {
    if (contentPane.getChildren().contains(nodeConnection)) {
      return;
    }
    Platform.runLater(() -> {
      contentPane.getChildren().add(nodeConnection);
      nodeConnection.getSource().toFront();
      nodeConnection.getTarget().toFront();
    });
  }

  private NodeState getNodeState(final State state) {
    return state.isInvariantOk() ? NodeState.VALID : NodeState.INVARIANT_VIOLATED;
  }

  public void reset() {
    synthesisContextService.currentOperationProperty().set(null);
    synthesisContextService.invariantViolatedProperty().set(false);
    getNodes().clear();
  }

  public StateNode containsStateNode(final StateNode stateNode) {
    if (SynthesisType.ACTION.equals(synthesisContextService.getSynthesisType())) {
      return null;
    }
    final Optional<BasicNode> optionalNode = nodes.stream()
        .filter(basicNode -> stateNode.getState() != null && stateNode.getState().getId()
            .equals(((StateNode) basicNode).getState().getId())).findFirst();
    return optionalNode.isPresent() ? (StateNode) optionalNode.get() : null;
  }

  public void expandAllNodes() {
    getNodes().forEach(basicNode -> basicNode.isExpandedProperty().set(true));
  }

  public void shrinkAllNodes() {
    getNodes().forEach(basicNode -> basicNode.isExpandedProperty().set(false));
  }
}
