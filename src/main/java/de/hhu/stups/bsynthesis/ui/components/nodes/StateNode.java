package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.animator.command.FindValidStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
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
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nullable;

public class StateNode extends BasicNode implements Initializable {

  public static final double WIDTH = 100;
  public static final double HEIGHT = 100;
  private static final double EXPANDED_WIDTH = 400;
  private static final double EXPANDED_HEIGHT = 300;

  private final ObjectProperty<State> stateProperty;
  private final SynthesisContextService synthesisContextService;
  private final StringProperty titleProperty;
  private final Timeline timeline;
  private final SetProperty<BasicNode> successorProperty;
  private final SetProperty<BasicNode> predecessorProperty;

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
                   final SynthesisContextService synthesisContextService,
                   final ValidationPane validationPane,
                   @Assisted @Nullable final State state,
                   @Assisted @Nullable final Trace trace,
                   @Assisted final Point2D position,
                   @Assisted final NodeState nodeState) {
    super(position, nodeState, validationPane, synthesisContextService.getNodeContextMenuFactory());
    this.synthesisContextService = synthesisContextService;
    timeline = new Timeline();
    stateProperty = new SimpleObjectProperty<>(state);
    titleProperty = new SimpleStringProperty();
    successorProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    predecessorProperty = new SimpleSetProperty<>(FXCollections.observableSet());

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

    tableViewState.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
    tableViewState.onMouseClickedProperty().addListener((observable, oldValue, newValue) ->
        toFront());
    setCompressedWidth();

    initializeSiblingConnections();
    initializeTableView();
    initializeTableColumn();

    setBackgroundColor();

    nodeStateProperty().addListener((observable, oldValue, newValue) -> initializeTableColumn());

    contentGridPane.getChildren().remove(tableViewState);

    isExpandedProperty().addListener((observable, oldValue, newValue) -> {
      contentGridPane.getChildren().remove(newValue ? lbTitle : tableViewState);
      contentGridPane.getChildren().add(newValue ? tableViewState : lbTitle);
    });

    synthesisContextService.machineVarNamesProperty().addListener(
        (observable, oldValue, newValue) -> initializeTableView());

    lbTitle.textProperty().bind(titleProperty());

    stateProperty.addListener((observable, oldValue, newValue) -> setTitle(newValue));

    nodeHeader.setBasicNode(this);

    setCompressedWidth();
    isExpandedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != oldValue) {
        resizeNode(newValue);
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
    if (NodeState.TENTATIVE.equals(nodeStateProperty().get())) {
      tableColumnInputState.setCellFactory(TextFieldTableCell.forTableColumn());
      tableColumnInputState.setOnEditCommit(
          (TableColumn.CellEditEvent<StateTableCell, String> event) ->
              Platform.runLater(() -> event.getTableView().getItems().get(
                  event.getTablePosition().getRow())
                  .setInputState(event.getNewValue())));
      tableViewState.setEditable(true);
      return;
    }
    tableViewState.setEditable(false);
  }

  private void initializeTableView() {
    tableViewState.getItems().clear();
    final ObservableSet<String> machineVarNames = synthesisContextService.getMachineVarNames();
    if (machineVarNames != null) {
      machineVarNames.forEach(machineVarName ->
          tableViewState.getItems().add(new StateTableCell(
              machineVarName, getState() != null && getState().isInitialised()
              ? getState().eval(machineVarName).toString() : "")));
    }
  }

  /**
   * Return the {@link StateNode predecessor node} of {@link this} using the {@link #traceProperty()
   * current trace}.
   */
  public StateNode getPredecessorFromTrace() {
    // TODO: eval operation instead of just using the trace from model checking
    final Trace trace = traceProperty().get();
    if (trace == null || !trace.canGoBack()) {
      return null;
    }
    final Trace newTrace = trace.back();
    final State state = newTrace.getCurrentState();
    return synthesisContextService.getStateNodeFactory()
        .create(state, newTrace, new Point2D(getXPosition() - getWidth() * 2, getYPosition()),
            state.isInvariantOk() ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
  }

  void findPredecessor() {
    final StateNode predecessorNode = getPredecessorFromTrace();
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
    return synthesisContextService.getStateNodeFactory()
        .create(state, newTrace, new Point2D(getXPosition() + getWidth() * 2, getYPosition()),
            state.isInvariantOk() ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
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
    final KeyFrame expandAnimation = new KeyFrame(Duration.millis(250),
        new KeyValue(nodeWidthProperty(), targetWidth),
        new KeyValue(nodeHeightProperty(), targetHeight));
    timeline.getKeyFrames().add(expandAnimation);
    timeline.play();
    adjustPositionIfNecessary();
    toFront();
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
  void validateState() {
    if (!validateInputValues(tableViewState.getItems())) {
      return;
    }
    // create equality predicate with variable values
    final Set<String> predicateStringSet = new HashSet<>(tableViewState.getItems().size());
    tableViewState.getItems().forEach(stateTableCell ->
        predicateStringSet.add(stateTableCell.getVarName() + "=" + stateTableCell.getInputState()));
    final String predicate = Joiner.on(" & ").join(predicateStringSet);
    final StateSpace stateSpace = synthesisContextService.stateSpaceProperty().get();
    final FindValidStateCommand findValidStateCommand =
        new FindValidStateCommand(stateSpace, new ClassicalB(predicate));
    stateSpace.execute(findValidStateCommand);

    final FindValidStateCommand.ResultType resultType = findValidStateCommand.getResult();
    if (resultType.equals(FindValidStateCommand.ResultType.ERROR)) {
      return;
    }
    if (resultType.equals(FindValidStateCommand.ResultType.NO_STATE_FOUND)) {
      nodeStateProperty().set(NodeState.INVARIANT_VIOLATED);
      checkDuplicatedNode();
      return;
    }
    final State state = stateSpace.getState(findValidStateCommand.getStateId());
    stateProperty.set(state);
    nodeStateProperty().set(stateProperty.get().isInvariantOk()
        ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
    checkDuplicatedNode();
  }

  /**
   * When a tentative {@link StateNode} is validated we check if the node already exists.
   * In this case we delete the new node and expand the existing equivalent node.
   */
  private void checkDuplicatedNode() {
    if (synthesisContextService.getSynthesisType().isAction()) {
      return;
    }
    // TODO: not working if a state violates the invariant since it has no id in
    // the current state space
    final String stateId = getState().getId();
    for (final BasicNode basicNode : getValidationPane().getNodes()) {
      final StateNode stateNode = ((StateNode) basicNode);
      if (!stateNode.equals(this) && stateNode.getState() != null
          && stateNode.getState().getId().equals(stateId)) {
        stateNode.isExpandedProperty().set(true);
        Platform.runLater(this::remove);
      }
    }
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

  public SetProperty<BasicNode> successorProperty() {
    return successorProperty;
  }

  public SetProperty<BasicNode> predecessorProperty() {
    return predecessorProperty;
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