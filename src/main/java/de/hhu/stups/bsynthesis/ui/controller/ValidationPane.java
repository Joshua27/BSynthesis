package de.hhu.stups.bsynthesis.ui.controller;

import static java.beans.Beans.isInstanceOf;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.prob.GetViolatingVarsFromExamplesCommand;
import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.services.ValidationPaneEvent;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.NodesFromTracePositionGenerator;
import de.hhu.stups.bsynthesis.ui.components.ValidationContextMenu;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.ValidationContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeLine;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;

import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The validation pane which is graphically split in two areas where the left side contains valid
 * example nodes (positive) and the right side invalid ones (negative).
 */
@Singleton
public class ValidationPane extends Pane implements Initializable {

  public static final double WIDTH = 3200.0;
  public static final double HEIGHT = 1800.0;

  private static final int MODEL_CHECKING_STATE_AMOUNT = 5;

  private static final String VALID_COLOR = "#C2FFC0";
  private static final String INVALID_COLOR = "#FFC0C0";

  private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ListProperty<BasicNode> nodes;
  private final SimpleDoubleProperty scaleFactorProperty;
  private final StateNodeFactory stateNodeFactory;
  private final ValidationContextMenu validationContextMenu;
  private final SynthesisContextService synthesisContextService;
  private final ModelCheckingService modelCheckingService;
  private final UiService uiService;

  private BasicNode dragNode;
  private double offsetX;
  private double offsetY;

  /**
   * Initialize variables from the injector and create the {@link ValidationContextMenu}.
   */
  @Inject
  public ValidationPane(final FXMLLoader loader,
                        final StateNodeFactory stateNodeFactory,
                        final ValidationContextMenuFactory validationContextMenuFactory,
                        final ServiceDelegator serviceDelegator) {
    this.stateNodeFactory = stateNodeFactory;
    modelCheckingService = serviceDelegator.modelCheckingService();
    synthesisContextService = serviceDelegator.synthesisContextService();
    uiService = serviceDelegator.uiService();

    scaleFactorProperty = new SimpleDoubleProperty(1.0);
    validationContextMenu = validationContextMenuFactory.create(SynthesisType.NONE);
    nodes = new SimpleListProperty<>(FXCollections.observableArrayList());
    Loader.loadFxml(loader, this, "validation_pane.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    validationContextMenu.setValidationPane(this);

    this.requestFocus();
    this.setPrefSize(WIDTH, HEIGHT);

    EasyBind.subscribe(synthesisContextService.stateSpaceProperty(), stateSpace -> {
      getNodes().clear();
      modelCheckingService.indicatorPresentProperty().set(false);
    });

    synthesisContextService.contextEventStream().subscribe(contextEvent -> {
      if (ContextEvent.RESET_CONTEXT.equals(contextEvent)) {
        getNodes().clear();
      }
    });

    handleNodeChanges();
    subscribeToUiEvents();
    initializeContextMenu();
    initializeDragEvents();
    initializeBackground();
  }

  private void subscribeToUiEvents() {
    uiService.addNodeConnectionEventSource().subscribe(this::addNodeConnection);
    uiService.applicationEventStream().subscribe(applicationEvent -> {
      if (applicationEvent.getApplicationEventType().isCloseApp()) {
        executorService.shutdown();
      }
    });
    uiService.validationPaneEventSource().subscribe(this::handleValidationPaneEvent);
  }

  private void handleValidationPaneEvent(final ValidationPaneEvent validationPaneEvent) {
    final BasicNode node = validationPaneEvent.getNode();
    switch (validationPaneEvent.getValidationPaneEventType()) {
      case CLEAR:
        nodes.clear();
        break;
      case SHOW_NODE:
        addNode(node);
        break;
      case REMOVE_NODE:
        nodes.remove(node);
        break;
      case ADJUST_NODE_POSITION:
        adjustPositionIfNecessary(node);
        break;
      case NODE_USER_VALIDATION:
        node.userValidationProperty().set(getExampleValidation(node));
        updateSynthesisType(node);
        break;
      case CHECK_DUPLICATE_NODE:
        if (node instanceof StateNode) {
          Platform.runLater(() -> {
            final StateNode stateNode = (StateNode) node;
            final StateNode equivalentNode = containsStateNode(stateNode);
            stateNode.equivalentNodeProperty().set(equivalentNode == null
                ? stateNode : equivalentNode);
          });
        }
        break;
      default:
        break;
    }
  }

  /**
   * Prevent {@link BasicNode} to leave the validation pane.
   */
  private void adjustPositionIfNecessary(final BasicNode basicNode) {
    final boolean isExpanded = basicNode.isExpandedProperty().get();
    final double currentWidth;
    final double currentHeight;
    if (basicNode instanceof StateNode) {
      currentWidth = isExpanded ? StateNode.EXPANDED_WIDTH : StateNode.WIDTH;
    } else {
      currentWidth = (isExpanded ? StateNode.EXPANDED_WIDTH : StateNode.WIDTH) * 2 + 100;
    }
    currentHeight = isExpanded ? StateNode.EXPANDED_HEIGHT : StateNode.HEIGHT;
    if (!isValidXPosition(basicNode.getXPosition(), currentWidth)) {
      if (basicNode.getXPosition() < 0) {
        basicNode.setXPosition(5.0);
      } else if (basicNode.getXPosition() + currentWidth > ValidationPane.WIDTH) {
        basicNode.setXPosition(ValidationPane.WIDTH - currentWidth - 20.0);
      }
    }
    if (!isValidYPosition(basicNode.getYPosition(), currentHeight)) {
      if (basicNode.getYPosition() < 0) {
        basicNode.setYPosition(5.0);
      } else if (basicNode.getYPosition() + currentHeight > ValidationPane.HEIGHT) {
        basicNode.setYPosition(ValidationPane.HEIGHT - currentHeight - 20.0);
      }
    }
  }

  private void handleNodeChanges() {
    nodes.addListener((ListChangeListener<BasicNode>) change -> {
      /*  add nodes to the pane when added to {@link #nodes} */
      while (change.next()) {
        if (change.wasAdded()) {
          change.getAddedSubList().forEach(node -> Platform.runLater(() ->
              this.getChildren().add(node)));
        } else if (change.wasRemoved()) {
          change.getRemoved().forEach(node -> Platform.runLater(() ->
              removeNode(node)));
        }
      }
    });
  }

  /**
   * Set the {@link SynthesisContextService#synthesisTypeProperty()} according to the current ui
   * state of the {@link #getNodes() nodes}.
   */
  private void updateSynthesisType(final BasicNode node) {
    if (node instanceof TransitionNode || node.isTentative()
        || synthesisContextService.synthesisTypeProperty().get().isAction()
        || lockSynthesisType()) {
      return;
    }
    // synthesis type is invariant if a violating state is set to be valid
    final Optional<BasicNode> optionalValidNode = getValidNodes().stream().filter(basicNode ->
        NodeState.INVARIANT_VIOLATED.equals(basicNode.nodeStateProperty().get())).findAny();
    if (optionalValidNode.isPresent()) {
      synthesisContextService.synthesisTypeProperty().set(SynthesisType.INVARIANT);
      return;
    }
    synthesisContextService.synthesisTypeProperty().set(SynthesisType.GUARD);
  }

  /**
   * Do not change the {@link SynthesisContextService#synthesisTypeProperty synthesis type } when
   * synthesis is running, has been suspended by providing a distinguishing example or aims at
   * repairing a deadlock (the user decides what to do with deadlocks).
   */
  private boolean lockSynthesisType() {
    return synthesisContextService.synthesisSuspendedProperty()
        .or(synthesisContextService.synthesisRunningProperty())
        .or(synthesisContextService.modifyInvariantsProperty())
        .or(modelCheckingService.deadlockRepairProperty().isNotNull()).get();
  }

  /**
   * Remove a {@link BasicNode} from the pane, delete the node from all ancestors and remove edges
   * with this node.
   */
  @SuppressWarnings("unused")
  private void removeNode(final BasicNode nodeToRemove) {
    Platform.runLater(() -> this.getChildren().remove(nodeToRemove));
    // remove node line connections
    this.getChildren().forEach(node -> {
      if (node instanceof NodeLine && (nodeToRemove.equals(((NodeLine) node).getSource())
          || nodeToRemove.equals(((NodeLine) node).getTarget()))) {
        Platform.runLater(() -> this.getChildren().remove(node));
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
    this.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
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

  private void initializeDragEvents() {
    this.setOnMousePressed(event -> {
      if (dragNode != null || event.getButton().equals(MouseButton.SECONDARY)) {
        return;
      }
      Node node = (Node) event.getTarget();
      if (isValidDragObject(node)) {
        while (node.getParent() != this) {
          node = node.getParent();
        }
        offsetX = node.getLayoutX() - event.getX();
        offsetY = node.getLayoutY() - event.getY();
        dragNode = (BasicNode) node;
        dragNode.toFront();
        childrenToFrontIfNecessary(dragNode);
      }
    });

    this.setOnMouseReleased(event -> dragNode = null);

    this.setOnMouseDragged(event -> {
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
    validRectangle.setFill(Color.web(ValidationPane.VALID_COLOR));
    this.getChildren().add(0, validRectangle);

    final Rectangle invalidRectangle;
    invalidRectangle = new Rectangle(halfWidth, 0, halfWidth, HEIGHT);
    invalidRectangle.setFill(Color.web(ValidationPane.INVALID_COLOR));
    this.getChildren().add(1, invalidRectangle);

    final Line splitLine;
    splitLine = new Line(halfWidth, 0.0, halfWidth, HEIGHT);
    splitLine.setStyle("-fx-stroke-dash-array: 0.1 5.0;");
    this.getChildren().add(2, splitLine);
  }

  /**
   * Initialize the nodes from the trace derived by the model checker in case the model is
   * defective. Determine the machine operation that leads into the erroneous state.
   */
  public void initializeNodesFromTrace() {
    final Trace initialMcTrace = modelCheckingService.errorTraceProperty().get();
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
    Platform.runLater(this::ignoreNonViolatingVarsIfNotDeadlock);
  }

  private void ignoreNonViolatingVarsIfNotDeadlock() {
    if (modelCheckingService.deadlockRepairProperty().get() != null) {
      return;
    }
    Platform.runLater(this::ignoreNonViolatingVars);
  }

  /**
   * Finding an erroneous state we search for those variables that violate an invariant and consider
   * the other machine variables to be ignored by setting the bindings in
   * {@link de.hhu.stups.bsynthesis.services.UiService#currentVarStatesMapProperty}.
   */
  private void ignoreNonViolatingVars() {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final GetViolatingVarsFromExamplesCommand getViolatingVarsFromExamplesCommand =
        new GetViolatingVarsFromExamplesCommand(getValidNodes(), getInvalidNodes(),
            synthesisContextService.machineVarNamesProperty().get());
    stateSpace.execute(getViolatingVarsFromExamplesCommand);
    final ObservableSet<String> violatingVarNames =
        getViolatingVarsFromExamplesCommand.getViolatingVarNames();
    logger.info("Invariant violating vars: " + violatingVarNames);
    uiService.currentVarStatesMapProperty().forEach((varName, booleanProperty) -> {
      if (violatingVarNames.contains(varName)) {
        booleanProperty.set(false);
      } else {
        booleanProperty.set(true);
      }
    });
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

  private boolean isValidXPosition(final double positionX, final double nodeWidth) {
    return positionX > 5.0 && positionX + nodeWidth < WIDTH - 5.0;
  }

  private boolean isValidYPosition(final double positionY, final double nodeHeight) {
    return positionY > 5.0 && positionY + nodeHeight < HEIGHT - 5.0;
  }

  private boolean isValidDragObject(final Node node) {
    return node != null && (isInstanceOf(node, BasicNode.class)
        && ((BasicNode) node).moveIsEnabledProperty().get()
        || isValidDragObject(node.getParent()));
  }

  /**
   * Return all validated nodes whose center is on the green side of the pane, i.e. in the left
   * half.
   */
  @SuppressWarnings("unused")
  public List<BasicNode> getValidNodes() {
    return nodes.stream().filter(BasicNode::userValidatedPositive).collect(Collectors.toList());
  }

  /**
   * Return all validated nodes whose center is on the red side of the pane, i.e. in the right half.
   */
  @SuppressWarnings("unused")
  public List<BasicNode> getInvalidNodes() {
    return nodes.stream().filter(BasicNode::userValidatedNegative).collect(Collectors.toList());
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
    if (node instanceof StateNode) {
      // state node
      executorService.execute(new Task<Boolean>() {
        @Override
        protected Boolean call() throws Exception {
          final StateNode stateNode = (StateNode) node;
          if (stateNode.isTentative()) {
            stateNode.validateState();
          }
          final StateNode equivalentNode = containsStateNode(stateNode);
          if (equivalentNode != null) {
            equivalentNode.highlightNodeEffect();
            return false;
          }
          Platform.runLater(() -> nodes.add(node));
          addStateNodeAncestors(stateNode);
          return true;
        }
      });
      return;
    }
    // transition node
    executorService.execute((new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        final TransitionNode transitionNode = (TransitionNode) node;
        transitionNode.validateTransition();
        Platform.runLater(() -> {
          final TransitionNode equivalentNode = containsTransitionNode(transitionNode);
          if (equivalentNode == null) {
            nodes.add(node);
          }
        });
        return true;
      }
    }));
  }

  /**
   * After adding the {@link StateNode} itself we set the corresponding connections to
   * its successor and predecessor nodes if present.
   */
  private void addStateNodeAncestors(final StateNode stateNode) {
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
  private void addNodeConnection(final NodeLine nodeConnection) {
    if (this.getChildren().contains(nodeConnection)) {
      return;
    }
    Platform.runLater(() -> {
      this.getChildren().add(nodeConnection);
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
  private StateNode containsStateNode(final StateNode stateNode) {
    if (synthesisContextService.getSynthesisType().isAction() || stateNode.getState() == null) {
      return null;
    }
    final Optional<BasicNode> optionalNode = nodes.stream()
        .filter(basicNode -> basicNode instanceof StateNode
            && ((StateNode) basicNode).getState() != null
            && !stateNode.equals(basicNode)
            && stateNode.getState().getId().equals(((StateNode) basicNode).getState().getId()))
        .findFirst();
    return (StateNode) optionalNode.orElse(null);
  }

  /**
   * Check if the validation pane already contains a specific {@link StateNode}. Return the node if
   * present otherwise return null.
   */
  private TransitionNode containsTransitionNode(final TransitionNode transitionNode) {
    if (!synthesisContextService.getSynthesisType().isAction()
        || transitionNode.getInputState() == null || transitionNode.getOutputState() == null) {
      return null;
    }
    final Optional<BasicNode> optionalNode = nodes.stream()
        .filter(basicNode -> !transitionNode.equals(basicNode)
            && transitionNode.getInputState().getId()
            .equals(((TransitionNode) basicNode).getInputState().getId())
            && ((TransitionNode) basicNode).getOutputState() != null
            && transitionNode.getOutputState().getId()
            .equals(((TransitionNode) basicNode).getOutputState().getId()))
        .findFirst();
    return (TransitionNode) optionalNode.orElse(null);
  }

  public void expandAllNodes() {
    getNodes().forEach(basicNode -> basicNode.isExpandedProperty().set(true));
  }

  public void shrinkAllNodes() {
    getNodes().forEach(basicNode -> basicNode.isExpandedProperty().set(false));
  }

  private boolean getExampleValidation(final BasicNode basicNode) {
    return basicNode.getXPosition() + basicNode.getWidth() / 2 < ValidationPane.WIDTH / 2;
  }

  /**
   * The model checker found a deadlock state s and the user decided to synthesize a new operation
   * or a adapt an existing one to transition from s to another state s' to resolve this deadlock.
   * Therefore, we add a {@link TransitionNode} having s as its input state. The output state as
   * well as other transitions need to be provided by the user.
   */
  public void initializeDeadlockResolveFromTrace() {
    final Trace initialMcTrace = modelCheckingService.errorTraceProperty().get();
    if (initialMcTrace == null) {
      return;
    }
    synthesisContextService.stateSpaceProperty().set(initialMcTrace.getStateSpace());
    final TransitionNode transitionNode = uiService.getTransitionNodeFactory()
        .create(initialMcTrace.getCurrentState(), null, new Point2D(100.0, 100.0),
            NodeState.TENTATIVE);
    transitionNode.inputStateFromModelChecking();
    addNode(transitionNode);
  }
}