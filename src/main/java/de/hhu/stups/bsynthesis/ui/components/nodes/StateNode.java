package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.animator.command.FindValidStateCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nullable;

public class StateNode extends BasicNode implements Initializable {

  public static final double WIDTH = 100;
  public static final double HEIGHT = 100;
  private static final double EXPANDED_WIDTH = 400;
  private static final double EXPANDED_HEIGHT = 300;

  private final ObjectProperty<State> stateProperty;
  private final MapProperty<String, String> tableViewStateMapProperty;
  private final SynthesisContextService synthesisContextService;
  private final StringProperty titleProperty;
  private final Timeline timeline;
  private final Timeline auxiliaryTimeline;
  private final SetProperty<BasicNode> successorProperty;
  private final SetProperty<BasicNode> predecessorProperty;
  private final BooleanProperty stateFromModelCheckingProperty;
  private final UiService uiService;

  @FXML
  @SuppressWarnings("unused")
  private GridPane contentGridPane;
  @FXML
  @SuppressWarnings("unused")
  private NodeHeader nodeHeader;
  @FXML
  @SuppressWarnings("unused")
  private Label lbTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableView<StateTableCell> tableViewState;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<StateTableCell, String> tableColumnVarName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<StateTableCell, String> tableColumnInputState;

  /**
   * Initialize the {@link BasicNode} and set the node specific properties as well as the default
   * position.
   */
  @Inject
  public StateNode(final FXMLLoader loader,
                   final ServiceDelegator serviceDelegator,
                   final ValidationPane validationPane,
                   @Assisted @Nullable final State state,
                   @Assisted @Nullable final Trace trace,
                   @Assisted final Point2D position,
                   @Assisted final NodeState nodeState) {
    super(position, nodeState, validationPane,
        serviceDelegator.uiService().getNodeContextMenuFactory());
    uiService = serviceDelegator.uiService();
    synthesisContextService = serviceDelegator.synthesisContextService();
    timeline = new Timeline();
    auxiliaryTimeline = new Timeline();
    stateProperty = new SimpleObjectProperty<>(state);
    titleProperty = new SimpleStringProperty();
    successorProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    predecessorProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    tableViewStateMapProperty = new SimpleMapProperty<>(FXCollections.observableHashMap());
    stateFromModelCheckingProperty = new SimpleBooleanProperty(false);

    setStyle("-fx-border-color: #8E8E8E");
    traceProperty().set(trace);
    setLayoutX(position.getX());
    setLayoutY(position.getY());
    adjustPositionIfNecessary();
    Loader.loadFxml(loader, this, "state_node.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    nodeWidthProperty().set(WIDTH);
    nodeHeightProperty().set(HEIGHT);

    setTitle(stateProperty.get());
    nodeHeader.setBasicNode(this);

    setCompressedWidth();

    initializeSiblingConnections();
    initializeTableView();
    initializeTableColumn();

    refreshBackgroundColor();

    nodeStateProperty().addListener((observable, oldValue, newValue) -> initializeTableColumn());

    contentGridPane.getChildren().remove(tableViewState);

    isExpandedProperty().addListener((observable, oldValue, newValue) ->
        Platform.runLater(() -> {
          contentGridPane.getChildren().remove(newValue ? lbTitle : tableViewState);
          contentGridPane.getChildren().add(newValue ? tableViewState : lbTitle);
        }));

    synthesisContextService.machineVarNamesProperty().addListener(
        (observable, oldValue, newValue) -> initializeTableView());

    lbTitle.textProperty().bind(titleProperty());

    stateProperty.addListener((observable, oldValue, newValue) -> {
      setTitle(newValue);
      initializeTableView();
    });

    nodeHeader.setBasicNode(this);

    setCompressedWidth();
    isExpandedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != oldValue) {
        Platform.runLater(() -> resizeNode(newValue));
      }
    });
  }

  /**
   * Validated states that are part of the state space have its id as the title, otherwise
   * "Unnamed".
   */
  private void setTitle(final State state) {
    titleProperty().set(state != null ? state.getId() : "Unnamed");
  }

  private void initializeSiblingConnections() {
    successorProperty().addListener((SetChangeListener<BasicNode>) change -> {
      if (change.wasAdded()) {
        getValidationPane().addNodeConnection(new NodeLine(this, change.getElementAdded()));
      }
    });
    predecessorProperty().addListener((SetChangeListener<BasicNode>) change -> {
      if (change.wasAdded()) {
        getValidationPane().addNodeConnection(new NodeLine(change.getElementAdded(), this));
      }
    });
  }

  private void initializeTableColumn() {
    // TODO: validate input on commit
    tableColumnInputState.setCellFactory(TextFieldTableCell.forTableColumn());
    tableColumnInputState.setOnEditCommit(
        (TableColumn.CellEditEvent<StateTableCell, String> event) ->
            Platform.runLater(() -> {
              nodeStateProperty().set(NodeState.TENTATIVE);
              event.getTableView().getItems().get(
                  event.getTablePosition().getRow())
                  .setInputState(event.getNewValue());
              updateTableViewStateMap();
            }));
    tableViewState.editableProperty().bind(stateFromModelCheckingProperty.not());
  }

  private void updateTableViewStateMap() {
    tableViewState.getItems().forEach(stateTableCell ->
        tableViewStateMapProperty.put(stateTableCell.getVarName(), stateTableCell.getInputState()));
  }

  private void initializeTableView() {
    tableViewState.disableProperty().bind(disableProperty());
    tableViewState.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
    tableViewState.onMouseClickedProperty().addListener((observable, oldValue, newValue) ->
        toFront());
    tableViewState.getItems().clear();
    final ObservableSet<String> machineVarNames = synthesisContextService.getMachineVarNames();
    if (machineVarNames != null) {
      machineVarNames.forEach(machineVarName -> {
        tableViewStateMapProperty.put(machineVarName, "");
        tableViewState.getItems().add(new StateTableCell(
            machineVarName, getState() != null && getState().isInitialised()
            ? getState().eval(machineVarName).toString() : ""));
      });
    }
  }

  /**
   * Return the {@link StateNode predecessor node} of {@link this} using the {@link #traceProperty()
   * current trace}.
   */
  public StateNode getPredecessorFromTrace() {
    final Trace trace = traceProperty().get();
    if (trace == null || !trace.canGoBack()) {
      return null;
    }
    final Trace newTrace = trace.back();
    final State state = newTrace.getCurrentState();
    final StateNode stateNode = uiService.getStateNodeFactory()
        .create(state, newTrace, new Point2D(getXPosition() - getWidth() * 2, getYPosition()),
            state.isInvariantOk() ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
    stateNode.stateFromModelCheckingProperty().set(true);
    return stateNode;
  }

  void findPredecessor() {
    final StateNode predecessorNode = getPredecessorFromTrace();
    if (predecessorNode == null) {
      return;
    }
    final StateNode nodeExists = getValidationPane().containsStateNode(predecessorNode);
    // check if node already exists and just set the ancestors
    if (nodeExists != null) {
      nodeExists.successorProperty().add(this);
      predecessorProperty().add(nodeExists);
      return;
    }
    predecessorNode.successorProperty().add(this);
    getValidationPane().addNode(predecessorNode);

    predecessorProperty().add(predecessorNode);
  }

  /**
   * Return the {@link StateNode succesor node} of {@link this} using the {@link #traceProperty()
   * current trace}.
   */
  public StateNode getSuccessorFromTrace() {
    final Trace trace = traceProperty().get();
    if (trace == null || !trace.canGoForward()) {
      return null;
    }
    final Trace newTrace = trace.forward();
    final State state = newTrace.getCurrentState();
    final StateNode stateNode = uiService.getStateNodeFactory()
        .create(state, newTrace, new Point2D(getXPosition() + getWidth() * 2, getYPosition()),
            state.isInvariantOk() ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
    stateNode.stateFromModelCheckingProperty.setValue(true);
    return stateNode;
  }

  void findSuccessor() {
    final StateNode successorNode = getSuccessorFromTrace();
    if (successorNode == null) {
      return;
    }
    final StateNode nodeExists = getValidationPane().containsStateNode(successorNode);
    // check if node already exists and just set the ancestors
    if (nodeExists != null) {
      nodeExists.predecessorProperty().add(this);
      successorProperty().add(nodeExists);
      return;
    }
    successorNode.predecessorProperty().add(this);
    getValidationPane().addNode(successorNode);

    successorProperty().add(successorNode);
  }

  private void setCompressedWidth() {
    setPrefWidth(WIDTH);
    setPrefHeight(HEIGHT);
  }

  private void resizeNode(final boolean expand) {
    timeline.getKeyFrames().clear();
    final double targetWidth = expand ? EXPANDED_WIDTH : WIDTH;
    final double targetHeight = expand ? EXPANDED_HEIGHT : HEIGHT;
    final KeyFrame expandAnimation = new KeyFrame(Duration.millis(250.0),
        new KeyValue(nodeWidthProperty(), targetWidth),
        new KeyValue(nodeHeightProperty(), targetHeight));
    timeline.getKeyFrames().add(expandAnimation);
    timeline.play();
    adjustPositionIfNecessary();
    toFront();
  }

  private void highlightNodeEffect() {
    toFront();
    if (!isExpanded()) {
      isExpandedProperty().set(true);
      return;
    }
    timeline.getKeyFrames().clear();

    final KeyFrame shrinkAnimation = new KeyFrame(Duration.millis(250.0),
        partialHighlightNodeFinished(),
        new KeyValue(nodeWidthProperty(), EXPANDED_WIDTH - EXPANDED_WIDTH / 4),
        new KeyValue(nodeHeightProperty(), EXPANDED_HEIGHT - EXPANDED_HEIGHT / 3));
    timeline.getKeyFrames().add(shrinkAnimation);
    timeline.play();
  }

  private EventHandler<ActionEvent> partialHighlightNodeFinished() {
    return event -> {
      auxiliaryTimeline.getKeyFrames().clear();
      final KeyFrame expandAnimation = new KeyFrame(Duration.millis(250.0),
          new KeyValue(nodeWidthProperty(), EXPANDED_WIDTH),
          new KeyValue(nodeHeightProperty(), EXPANDED_HEIGHT));
      auxiliaryTimeline.getKeyFrames().clear();
      auxiliaryTimeline.getKeyFrames().add(expandAnimation);
      auxiliaryTimeline.play();
    };
  }

  private void adjustPositionIfNecessary() {
    final double currentWidth = isExpanded() ? EXPANDED_WIDTH : WIDTH;
    final double currentHeight = isExpanded() ? EXPANDED_HEIGHT : HEIGHT;
    if (!getValidationPane().isValidXPosition(getXPosition(), currentWidth)) {
      if (getXPosition() < 0) {
        setXPosition(5.0);
      } else if (getXPosition() + currentWidth > ValidationPane.WIDTH) {
        setXPosition(ValidationPane.WIDTH - currentWidth - 5.0);
      }
    }
    if (!getValidationPane().isValidYPosition(getYPosition(), currentHeight)) {
      if (getYPosition() < 0) {
        setYPosition(5.0);
      } else if (getYPosition() + currentHeight > ValidationPane.HEIGHT) {
        setYPosition(ValidationPane.HEIGHT - currentHeight - 5.0);
      }
    }
  }

  /**
   * Return true if the state is validated to be valid by the user otherwise false.
   */
  public boolean isValidatedPositive() {
    return getXPosition() < ValidationPane.WIDTH / 2;
  }

  public State getState() {
    return stateProperty.get();
  }

  ObjectProperty<State> stateProperty() {
    return stateProperty;
  }

  @SuppressWarnings("WeakerAccess")
  StringProperty titleProperty() {
    return titleProperty;
  }

  @Override
  public void remove() {
    getValidationPane().getNodes().remove(this);
  }

  /**
   * Given a {@link NodeState#TENTATIVE} state node we validate the state by checking the invariant
   * on the values which sets the type either to {@link NodeState#VALID} or {@link
   * NodeState#INVARIANT_VIOLATED}.
   */
  public void validateState() {
    if (!validateInputValues(tableViewState.getItems())) {
      return;
    }
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    // create equality predicate with variable values
    final FindValidStateCommand findValidStateCommand =
        new FindValidStateCommand(stateSpace, getStateEqualityPredicate());
    stateSpace.execute(findValidStateCommand);

    if (!checkDuplicatedNode()) {
      final FindValidStateCommand.ResultType resultType = findValidStateCommand.getResult();
      if (resultType.equals(FindValidStateCommand.ResultType.ERROR)) {
        return;
      }
      if (resultType.equals(FindValidStateCommand.ResultType.NO_STATE_FOUND)) {
        nodeStateProperty().set(NodeState.INVARIANT_VIOLATED);
        return;
      }
      // check for duplicated state before setting the state property
      stateProperty.set(stateSpace.getState(findValidStateCommand.getStateId()));
      nodeStateProperty().set(stateProperty.get().isInvariantOk()
          ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
    }
  }

  /**
   * When a tentative {@link StateNode} is validated we check if the node already exists.
   * In this case we delete the new node and expand the existing equivalent node.
   */
  private boolean checkDuplicatedNode() {
    if (synthesisContextService.getSynthesisType().isAction()) {
      return false;
    }
    final Optional<StateNode> optionalStateNode = getValidationPane().getNodes().stream()
        .map(basicNode -> (StateNode) basicNode)
        .filter(stateNode -> stateNode != this && equalStates(stateNode)).findFirst();
    if (optionalStateNode.isPresent()) {
      remove();
      optionalStateNode.get().highlightNodeEffect();
      return true;
    }
    return false;
  }

  /**
   * Check if all variable values are filled.
   */
  private boolean validateInputValues(final ObservableList<StateTableCell> stateTableCells) {
    // TODO: validate types and well-definedness
    for (final StateTableCell stateTableCell : stateTableCells) {
      if (stateTableCell.getInputState().replaceAll("\\s+", "").isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Two {@link StateNode}'s are equal if they have exactly the same state values. We can't directly
   * compare {@link State} objects since we can also have erroneous states that are not added to the
   * state space and thus missing a corresponding state object.
   */
  private boolean equalStates(final StateNode stateNode) {
    final ObservableMap<String, String> tableViewStateMap = tableViewStateMapProperty.get();
    if (stateNode == null) {
      return false;
    }
    for (final Map.Entry<IEvalElement, AbstractEvalResult>
        entry : stateNode.getState().getValues().entrySet()) {
      final String keyCode = entry.getKey().getCode();
      if (!tableViewStateMap.containsKey(keyCode)
          || !tableViewStateMap.get(keyCode).equals(((EvalResult) entry.getValue()).getValue())) {
        return false;
      }
    }
    return true;
  }

  public BooleanProperty stateFromModelCheckingProperty() {
    return stateFromModelCheckingProperty;
  }

  public SetProperty<BasicNode> successorProperty() {
    return successorProperty;
  }

  public SetProperty<BasicNode> predecessorProperty() {
    return predecessorProperty;
  }

  /**
   * Compute and return the {@link this state node}'s predecessor state if possible otherwise null.
   */
  public State getPredecessor() {
    final StateNode predecessorStateNode = getPredecessorFromTrace();
    if (predecessorStateNode != null) {
      return predecessorStateNode.getState();
    }
    final Trace currentTrace = traceProperty().get();
    if (!currentTrace.canGoBack()) {
      return null;
    }
    final AnimationSelector animationSelector = synthesisContextService.getAnimationSelector();
    animationSelector.addNewAnimation(currentTrace.back());
    return animationSelector.getCurrentTrace().getCurrentState();
  }

  private IEvalElement getStateEqualityPredicate() {
    final Set<String> predicateStringSet = new HashSet<>(tableViewState.getItems().size());
    tableViewState.getItems().forEach(stateTableCell ->
        predicateStringSet.add(stateTableCell.getVarName() + "=" + stateTableCell.getInputState()));
    final String predicate = Joiner.on(" & ").join(predicateStringSet);
    return new ClassicalB(predicate);
  }

  /**
   * State table cell.
   */
  @SuppressWarnings( {"unused", "WeakerAccess"})
  public static final class StateTableCell {
    private final StringProperty varName;
    private final StringProperty inputState;

    public StateTableCell(final String varName,
                          final String inputState) {
      this.varName = new SimpleStringProperty(this, "varName", varName);
      this.inputState = new SimpleStringProperty(this, "inputState", inputState);
    }

    public String getVarName() {
      return varName.get();
    }

    public void setVarName(final String varName) {
      this.varName.set(varName);
    }

    public ReadOnlyStringProperty varNameProperty() {
      return varName;
    }

    public String getInputState() {
      return inputState.get();
    }

    public void setInputState(final String inputState) {
      this.inputState.set(inputState);
    }

    public ReadOnlyStringProperty inputStateProperty() {
      return inputState;
    }
  }
}