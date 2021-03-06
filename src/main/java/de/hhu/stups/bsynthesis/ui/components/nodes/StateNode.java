package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.services.DaemonThread;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.services.ValidationPaneEvent;
import de.hhu.stups.bsynthesis.services.ValidationPaneEventType;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
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
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nullable;

public class StateNode extends BasicNode implements Initializable {

  public static final double WIDTH = 100;
  public static final double HEIGHT = 100;
  public static final double EXPANDED_WIDTH = 400;
  public static final double EXPANDED_HEIGHT = 300;

  private final ObjectProperty<State> stateProperty;
  private final MapProperty<String, String> tableViewStateMapProperty;
  private final SynthesisContextService synthesisContextService;
  private final StringProperty titleProperty;
  private final SetProperty<BasicNode> successorProperty;
  private final SetProperty<BasicNode> predecessorProperty;
  private final BooleanProperty stateFromModelCheckingProperty;
  private final UiService uiService;
  private final ObjectProperty<StateNode> equivalentNodeProperty;

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
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<StateTableCell, CheckBox> tableColumnIgnoreVar;

  /**
   * Initialize the {@link BasicNode} and set the node specific properties as well as the default
   * position.
   */
  @Inject
  public StateNode(final FXMLLoader loader,
                   final ServiceDelegator serviceDelegator,
                   @Assisted @Nullable final State state,
                   @Assisted @Nullable final Trace trace,
                   @Assisted final Point2D position,
                   @Assisted final NodeState nodeState) {
    super(position, nodeState, serviceDelegator.uiService());
    uiService = serviceDelegator.uiService();
    synthesisContextService = serviceDelegator.synthesisContextService();
    stateProperty = new SimpleObjectProperty<>(state);
    titleProperty = new SimpleStringProperty();
    successorProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    predecessorProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    tableViewStateMapProperty = new SimpleMapProperty<>(FXCollections.observableHashMap());
    stateFromModelCheckingProperty = new SimpleBooleanProperty(false);
    equivalentNodeProperty = new SimpleObjectProperty<>();

    traceProperty().set(trace);
    setLayoutX(position.getX());
    setLayoutY(position.getY());
    Loader.loadFxml(loader, this, "state_node.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    uiService.validationPaneEventSource().push(
        new ValidationPaneEvent(ValidationPaneEventType.ADJUST_NODE_POSITION, this));
    nodeWidthProperty().set(WIDTH);
    nodeHeightProperty().set(HEIGHT);

    setTitle(stateProperty.get());
    nodeHeader.setBasicNode(this);

    setCompressedWidth();

    initializeSiblingConnections();
    initializeTableView();
    initializeTableColumn();

    refreshBackgroundColor();

    EasyBind.subscribe(nodeStateProperty(), nodeState -> initializeTableColumn());

    contentGridPane.getChildren().remove(tableViewState);

    EasyBind.subscribe(isExpandedProperty(), isExpanded -> Platform.runLater(() -> {
      if (isExpanded) {
        contentGridPane.getChildren().remove(lbTitle);
        addToContentGridPane(tableViewState);
      } else {
        contentGridPane.getChildren().remove(tableViewState);
        addToContentGridPane(lbTitle);
      }
      resizeNode(isExpanded);
    }));

    synthesisContextService.machineVarNamesProperty().addListener(
        (observable, oldValue, newValue) -> initializeTableView());

    lbTitle.textProperty().bind(titleProperty());

    EasyBind.subscribe(stateProperty, state -> {
      setTitle(state);
      initializeTableView();
    });

    nodeHeader.setBasicNode(this);

    setCompressedWidth();

    EasyBind.subscribe(equivalentNodeProperty, equivalentNode -> {
      if (equivalentNode != null && equivalentNode != this) {
        equivalentNode.highlightNodeEffect();
        remove();
      }
    });
  }

  private void addToContentGridPane(final Node node) {
    if (!contentGridPane.getChildren().contains(node)) {
      contentGridPane.getChildren().add(node);
    }
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
        uiService.addNodeConnectionEventSource().push(new NodeLine(this, change.getElementAdded()));
      }
    });
    predecessorProperty().addListener((SetChangeListener<BasicNode>) change -> {
      if (change.wasAdded()) {
        uiService.addNodeConnectionEventSource().push(new NodeLine(change.getElementAdded(), this));
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
    EasyBind.subscribe(tableViewState.onMouseClickedProperty(), eventHandler -> toFront());
    tableViewState.getItems().clear();
    final ObservableSet<String> machineVarNames = synthesisContextService.getMachineVarNames();
    if (machineVarNames != null) {
      machineVarNames.forEach(machineVarName -> {
        tableViewStateMapProperty.put(machineVarName, "");
        tableViewState.getItems().add(new StateTableCell(
            machineVarName, getState() != null && getState().isInitialised()
            ? getState().eval(machineVarName, FormulaExpand.EXPAND).toString() : "",
            uiService.currentVarStatesMapProperty().get(machineVarName)));
      });
    }
    tableColumnVarName.setCellValueFactory(param -> param.getValue().varNameProperty());
    tableColumnInputState.setCellValueFactory(param -> param.getValue().inputStateProperty());
    tableColumnIgnoreVar.setCellValueFactory(arg0 -> {
      final StateTableCell stateTableCell = arg0.getValue();
      final CheckBox checkBox = new CheckBox();
      checkBox.selectedProperty().bindBidirectional(stateTableCell.ignoreVarProperty());
      return new SimpleObjectProperty<>(checkBox);
    });
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
    uiService.validationPaneEventSource().push(
        new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, predecessorNode));
  }

  public ObjectProperty<StateNode> equivalentNodeProperty() {
    return equivalentNodeProperty;
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
    uiService.validationPaneEventSource().push(
        new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, successorNode));
  }

  private void setCompressedWidth() {
    setPrefWidth(WIDTH);
    setPrefHeight(HEIGHT);
  }

  private void resizeNode(final boolean expand) {
    toFront();
    final Timeline timeline = new Timeline();
    final double targetWidth = expand ? EXPANDED_WIDTH : WIDTH;
    final double targetHeight = expand ? EXPANDED_HEIGHT : HEIGHT;
    final KeyFrame expandAnimation = new KeyFrame(Duration.millis(250.0),
        new KeyValue(nodeWidthProperty(), targetWidth),
        new KeyValue(nodeHeightProperty(), targetHeight));
    timeline.getKeyFrames().add(expandAnimation);
    timeline.play();
    EasyBind.subscribe(timeline.onFinishedProperty(), actionEventEventHandler ->
        uiService.validationPaneEventSource().push(
            new ValidationPaneEvent(ValidationPaneEventType.ADJUST_NODE_POSITION, this)));
  }

  /**
   * Highlight the node with an resize effect.
   */
  public void highlightNodeEffect() {
    Platform.runLater(this::toFront);
    if (!isExpanded()) {
      Platform.runLater(() -> isExpandedProperty().set(true));
      return;
    }
    final Timeline timeline = new Timeline();

    final KeyFrame shrinkAnimation = new KeyFrame(Duration.millis(250.0),
        partialHighlightNodeFinished(),
        new KeyValue(nodeWidthProperty(), EXPANDED_WIDTH - EXPANDED_WIDTH / 4),
        new KeyValue(nodeHeightProperty(), EXPANDED_HEIGHT - EXPANDED_HEIGHT / 3));
    timeline.getKeyFrames().add(shrinkAnimation);
    timeline.play();
  }

  private EventHandler<ActionEvent> partialHighlightNodeFinished() {
    return event -> {
      final Timeline timeline = new Timeline();
      timeline.getKeyFrames().clear();
      final KeyFrame expandAnimation = new KeyFrame(Duration.millis(250.0),
          new KeyValue(nodeWidthProperty(), EXPANDED_WIDTH),
          new KeyValue(nodeHeightProperty(), EXPANDED_HEIGHT));
      timeline.getKeyFrames().clear();
      timeline.getKeyFrames().add(expandAnimation);
      timeline.play();
    };
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
    uiService.validationPaneEventSource().push(
        new ValidationPaneEvent(ValidationPaneEventType.REMOVE_NODE, this));
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
    final FindStateCommand findStateCommand =
        new FindStateCommand(stateSpace, getStateEqualityPredicate(), false);
    DaemonThread.getDaemonThread(() -> {
      stateSpace.execute(findStateCommand);
      final FindStateCommand.ResultType resultType = findStateCommand.getResult();
      if (resultType.equals(FindStateCommand.ResultType.ERROR)) {
        return;
      }
      if (resultType.equals(FindStateCommand.ResultType.NO_STATE_FOUND)) {
        nodeStateProperty().set(NodeState.INVARIANT_VIOLATED);
        return;
      }
      Platform.runLater(() -> {
        stateProperty.set(stateSpace.getState(findStateCommand.getStateId()));
        nodeStateProperty().set(stateProperty.get().isInvariantOk()
            ? NodeState.VALID : NodeState.INVARIANT_VIOLATED);
      });

      if (!synthesisContextService.getSynthesisType().isAction()) {
        uiService.validationPaneEventSource().push(
            new ValidationPaneEvent(ValidationPaneEventType.CHECK_DUPLICATE_NODE, this));
      }
    }).start();
  }

  /**
   * Check if all variable values are filled that are not set to be ignored.
   */
  private boolean validateInputValues(final ObservableList<StateTableCell> stateTableCells) {
    // TODO: validate types and well-definedness
    for (final StateTableCell stateTableCell : stateTableCells) {
      if (!stateTableCell.ignoreVarProperty().get()
          && stateTableCell.getInputState().replaceAll("\\s+", "").isEmpty()) {
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
   * Create equality nodes of the machine variable values that are not set to be ignored.
   */
  private IEvalElement getStateEqualityPredicate() {
    final Set<String> predicateStringSet = new HashSet<>(tableViewState.getItems().size());
    tableViewState.getItems().forEach(stateTableCell -> {
      if (!stateTableCell.ignoreVarProperty().get()) {
        predicateStringSet.add(stateTableCell.getVarName() + "=" + stateTableCell.getInputState());
      }
    });
    final String predicate = Joiner.on(" & ").join(predicateStringSet);
    return new ClassicalB(predicate, FormulaExpand.EXPAND);
  }
}