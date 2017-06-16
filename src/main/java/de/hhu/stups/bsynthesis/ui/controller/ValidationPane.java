package de.hhu.stups.bsynthesis.ui.controller;

import static java.beans.Beans.isInstanceOf;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingProgressIndicator;
import de.hhu.stups.bsynthesis.ui.components.NodesFromTracePositionGenerator;
import de.hhu.stups.bsynthesis.ui.components.SynthesisInfoBox;
import de.hhu.stups.bsynthesis.ui.components.ValidationContextMenu;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.ValidationContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeLine;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;

import de.prob.statespace.State;
import de.prob.statespace.Trace;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The validation pane which is graphically split in two areas where the left side contains valid
 * nodes and the right side invalid ones.
 */
@Singleton
public class ValidationPane extends ScrollPane implements Initializable {

  public static final double WIDTH = 2700.0;
  public static final double HEIGHT = 1600.0;

  private static final double MAX_ZOOM_IN = 1.0;
  private static final double MAX_ZOOM_OUT = 0.3;
  private static final int MODEL_CHECKING_STATE_AMOUNT = 5;

  private final ListProperty<BasicNode> nodes;

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

  /**
   * Initialize variables from the injector and create the {@link ValidationContextMenu}.
   */
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
    validationContextMenu = validationContextMenuFactory.create(SynthesisType.NONE);
    nodes = new SimpleListProperty<>(FXCollections.observableArrayList());

    Loader.loadFxml(loader, this, "validation_pane.fxml");
  }

  /**
   * Set the {@link SynthesisContextService#synthesisTypeProperty()} according to the current ui
   * state of the {@link #getNodes() nodes}.
   */
  private void updateSynthesisType(final BasicNode node) {
    if (node instanceof TransitionNode || node.isTentative()
        || SynthesisType.ACTION.equals(synthesisContextService.synthesisTypeProperty().get())) {
      return;
    }
    final Optional<BasicNode> optionalValidNode = getValidNodes().stream().filter(basicNode ->
        NodeState.INVARIANT_VIOLATED.equals(basicNode.nodeStateProperty().get())).findAny();
    final Optional<BasicNode> optionalInvalidNode = getInvalidNodes().stream().filter(basicNode ->
        NodeState.VALID.equals(basicNode.nodeStateProperty().get())).findAny();
    if (optionalValidNode.isPresent() || optionalInvalidNode.isPresent()) {
      synthesisContextService.synthesisTypeProperty().set(SynthesisType.INVARIANT);
      return;
    }
    synthesisContextService.synthesisTypeProperty().set(SynthesisType.GUARD);
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

    synthesisContextService.contextEventStream().subscribe(contextEvent -> {
      if (ContextEvent.RESET_CONTEXT.equals(contextEvent)) {
        getNodes().clear();
      }
    });

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
    // remove node line connections
    contentPane.getChildren().forEach(node -> {
      if (node instanceof NodeLine && (nodeToRemove.equals(((NodeLine) node).getSource())
          || nodeToRemove.equals(((NodeLine) node).getTarget()))) {
        Platform.runLater(() -> contentPane.getChildren().remove(node));
      }
    });
    // remove ancestors if necessary
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
          || preventShowingContextMenu(event)) {
        validationContextMenu.hide();
        return;
      }
      validationContextMenu.getGraphicPositionProperty()
          .setValue(new Point2D(event.getX(), event.getY()));
      validationContextMenu.show(this, event.getScreenX(), event.getScreenY());
    });
  }

  /**
   * Don't show a context menu when {@link SynthesisContextService#stateSpaceProperty() no machine
   * is loaded}, the {@link SynthesisContextService#synthesisTypeProperty()} is undefined, or a
   * {@link BasicNode} is present at the current position.
   */
  private boolean preventShowingContextMenu(final MouseEvent event) {
    return !event.getButton().equals(MouseButton.SECONDARY)
        || isInstanceOf(event.getTarget(), BasicNode.class)
        || synthesisContextService.getStateSpace() == null
        || synthesisContextService.synthesisTypeProperty().get().isUndefined();
  }

  private void initializeModelCheckingIndicator() {
    modelCheckingProgressIndicator.setValidationPane(this);
    modelCheckingProgressIndicator.setTranslateZ(1);
    modelCheckingProgressIndicator.updatePosition();

    modelCheckingProgressIndicator.indicatorPresentProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue
              && !contentAnchorPane.getChildren().contains(modelCheckingProgressIndicator)) {
            Platform.runLater(() ->
                contentAnchorPane.getChildren().add(modelCheckingProgressIndicator));
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
        Platform.runLater(this::initializeNodesFromTrace);
      }
    });
  }

  private void removeModelCheckingIndicator() {
    Platform.runLater(() -> contentAnchorPane.getChildren().remove(modelCheckingProgressIndicator));
  }

  private void initializeScaleEvents() {
    zoomGroup.scaleXProperty().bind(scaleFactorProperty);
    zoomGroup.scaleYProperty().bind(scaleFactorProperty);
    addEventFilter(ScrollEvent.ANY, event -> {
      if (!event.isControlDown()) {
        return;
      }
      if (event.getDeltaY() > 0 && (scaleFactorProperty.get() < MAX_ZOOM_IN)) {
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
      stateNode.stateFromModelCheckingProperty().set(true);
      final StateNode previousNode = nodesFromTraceGenerator.getPreviousNode();
      final Trace previousTrace = nodesFromTraceGenerator.getPreviousTrace();
      if (previousNode != null) {
        stateNode.successorProperty().add(previousNode);
        previousNode.predecessorProperty().add(stateNode);
      }
      // find operation that violates the invariant when following the trace
      if (!invariantViolatingOpIsSet && isViolatingOperation(previousNode, stateNode)) {
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

  /**
   * Check if we transitioned from a valid to a violating state.
   */
  private boolean isViolatingOperation(final StateNode previousNode,
                                       final StateNode stateNode) {
    return previousNode != null
        && !previousNode.getState().isInvariantOk()
        && stateNode.getState().isInvariantOk();
  }

  private boolean isValidZoom(final double scaleFactor) {
    return (scaleFactorProperty.get() > 0 || scaleFactor > 1.0)
        && (scaleFactorProperty.get() < 2.0 || scaleFactor < 1.0);
  }

  public boolean isValidXPosition(final double positionX, final double nodeWidth) {
    return positionX > 5.0 && positionX + nodeWidth < WIDTH - 5.0;
  }

  public boolean isValidYPosition(final double positionY, final double nodeHeight) {
    return positionY > 5.0 && positionY + nodeHeight < HEIGHT - 5.0;
  }

  private boolean isValidDragObject(final Node node) {
    return node != null && (isInstanceOf(node, BasicNode.class)
        && ((BasicNode) node).moveIsEnabledProperty().get()
        || isValidDragObject(node.getParent()));
  }

  /**
   * Return all validated nodes whose center is on the green side of the pane, i.e. in the left
   * half. Tentative nodes are ignored.
   */
  @SuppressWarnings("unused")
  public List<BasicNode> getValidNodes() {
    return nodes.stream()
        .filter(node -> !node.isTentative()
            && node.getXPosition() + node.getWidth() / 2 < ValidationPane.WIDTH / 2)
        .collect(Collectors.toList());
  }

  /**
   * Return all validated nodes whose center is on the red side of the pane, i.e. in the right half.
   * Tentative nodes are ignored.
   */
  @SuppressWarnings("unused")
  public List<BasicNode> getInvalidNodes() {
    return nodes.stream()
        .filter(node -> !node.isTentative()
            && node.getXPosition() + node.getWidth() / 2 >= ValidationPane.WIDTH / 2)
        .collect(Collectors.toList());
  }

  public ListProperty<BasicNode> getNodes() {
    return nodes;
  }

  @SuppressWarnings("unused")
  private void setScaleFactor(final double scaleFactor) {
    if (isValidZoom(scaleFactor)) {
      Platform.runLater(() -> scaleFactorProperty.set(scaleFactor));
    }
  }

  /**
   * When adding a {@link StateNode} to the pane check if ancestors exist and set the corresponding
   * relations.
   */
  public void addNode(final BasicNode node) {
    Platform.runLater(() -> nodes.add(node));
    if (node instanceof StateNode) {
      final StateNode stateNode = (StateNode) node;
      addStateNode(stateNode);
      stateNode.layoutXProperty().addListener((observable, oldValue, newValue) ->
          updateSynthesisType(stateNode));
      stateNode.layoutYProperty().addListener((observable, oldValue, newValue) ->
          updateSynthesisType(stateNode));
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

  /**
   * Add a {@link NodeLine} to the validation pane.
   */
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

  /**
   * Reset the validation pane and {@link SynthesisContextService context information} referring to
   * a specific operation.
   */
  public void reset() {
    synthesisContextService.currentOperationProperty().set(null);
    synthesisContextService.invariantViolatedProperty().set(false);
    getNodes().clear();
  }

  /**
   * Check if the validation pane already contains a specific {@link StateNode}. Return the node if
   * present otherwise return null.
   */
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
