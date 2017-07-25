package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.prob.statespace.State;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

import javax.annotation.Nullable;

public class TransitionNode extends BasicNode implements Initializable {

  private final ObjectProperty<State> inputStateProperty;
  private final ObjectProperty<State> outputStateProperty;

  @FXML
  @SuppressWarnings("unused")
  private StateNode inputStateNode;
  @FXML
  @SuppressWarnings("unused")
  private StateNode outputStateNode;

  /**
   * Initialize the {@link BasicNode}, set the {@link #inputStateProperty} and {@link
   * #outputStateProperty} and load the fxml resource.
   */
  @Inject
  public TransitionNode(final FXMLLoader loader,
                        final UiService uiService,
                        @Assisted("inputState") @Nullable final State inputState,
                        @Assisted("outputState") @Nullable final State outputState,
                        @Assisted final Point2D position,
                        @Assisted final NodeState nodeState) {
    super(position, nodeState, uiService);
    inputStateProperty = new SimpleObjectProperty<>(inputState);
    outputStateProperty = new SimpleObjectProperty<>(outputState);
    transparentBackgroundProperty().set(true);
    loader.setBuilderFactory(type -> {
      if (type.equals(StateNode.class)) {
        return () -> uiService.getStateNodeFactory().create(null,
            traceProperty().get(), new Point2D(0, 0), nodeStateProperty().get());
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });
    Loader.loadFxml(loader, this, "transition_node.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    initializeStateNodes();
    initializeNodeStateListener();
    setUserValidationHighlighting(userValidationProperty().get());

    userValidationProperty().addListener((observable, oldValue, newValue) ->
        setUserValidationHighlighting(newValue));
    outputStateNode.disableProperty().bind(userValidationProperty().not());

    prefWidthProperty().bind(inputStateNode.widthProperty()
        .add(outputStateNode.widthProperty()).add(100.0));
    prefHeightProperty().bind(inputStateNode.heightProperty());

    StackPane.setAlignment(inputStateNode, Pos.CENTER_LEFT);
    StackPane.setAlignment(outputStateNode, Pos.CENTER_RIGHT);
  }

  private void setUserValidationHighlighting(final boolean positive) {
    if (positive) {
      getStyleClass().remove("transitionNodeInvalid");
      getStyleClass().add("transitionNodeValid");
      return;
    }
    getStyleClass().remove("transitionNodeValid");
    getStyleClass().add("transitionNodeInvalid");
  }

  private void initializeNodeStateListener() {
    inputStateNode.nodeStateProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.isTentative() && !outputStateNode.isTentative()) {
        nodeStateProperty().set(NodeState.VALID);
      }
    });
    outputStateNode.nodeStateProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.isTentative() && !inputStateNode.isTentative()) {
        nodeStateProperty().set(NodeState.VALID);
      }
    });
  }

  private void initializeStateNodes() {
    inputStateNode.stateProperty().set(inputStateProperty.get());
    inputStateNode.titleProperty().set("Input");
    inputStateNode.nodeWidthProperty().set(100);
    inputStateNode.nodeHeightProperty().set(100);
    inputStateNode.moveIsEnabledProperty().set(false);
    inputStateNode.stateProperty().bindBidirectional(inputStateProperty);

    isExpandedProperty().bindBidirectional(inputStateNode.isExpandedProperty());

    outputStateNode.stateProperty().set(outputStateProperty.get());
    outputStateNode.setXPosition(inputStateNode.widthProperty().add(100.0).get());
    outputStateNode.setYPosition(0);
    outputStateNode.titleProperty().set("Output");
    outputStateNode.nodeWidthProperty().set(100);
    outputStateNode.nodeHeightProperty().set(100);
    outputStateNode.moveIsEnabledProperty().set(false);
    outputStateNode.stateProperty().bindBidirectional(outputStateProperty);
    outputStateNode.positionXProperty().bind(inputStateNode.widthProperty().add(100.0));

    inputStateNode.isExpandedProperty().bindBidirectional(outputStateNode.isExpandedProperty());
  }

  public State getInputState() {
    return inputStateProperty.get();
  }

  public State getOutputState() {
    return outputStateProperty.get();
  }

  public void childrenToFront() {
    inputStateNode.toFront();
    outputStateNode.toFront();
  }

  public void validateTransition() {
    validateInputState();
    validateOutputState();
  }

  private void validateInputState() {
    inputStateNode.validateState();
  }

  private void validateOutputState() {
    outputStateNode.validateState();
  }
}