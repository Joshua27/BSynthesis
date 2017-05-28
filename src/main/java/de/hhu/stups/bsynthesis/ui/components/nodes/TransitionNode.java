package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.statespace.State;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

public class TransitionNode extends BasicNode implements Initializable {

  private final ObjectProperty<State> inputStateProperty;
  private final ObjectProperty<State> outputStateProperty;
  private final SynthesisContextService synthesisContextService;

  private StateNode inputStateNode;
  private StateNode outputStateNode;

  /**
   * Initialize the {@link BasicNode}, set the {@link #inputStateProperty} and {@link
   * #outputStateProperty} and load the fxml resource.
   */
  @Inject
  public TransitionNode(final FXMLLoader loader,
                        final SynthesisContextService synthesisContextService,
                        final ValidationPane validationPane,
                        @Assisted("inputState") @Nullable final State inputState,
                        @Assisted("outputState") @Nullable final State outputState,
                        @Assisted final Point2D position,
                        @Assisted final NodeState nodeState) {
    super(position, nodeState, validationPane, synthesisContextService.getNodeContextMenuFactory());
    this.synthesisContextService = synthesisContextService;
    inputStateProperty = new SimpleObjectProperty<>(inputState);
    outputStateProperty = new SimpleObjectProperty<>(outputState);

    loader.setLocation(getClass().getResource("transition_node.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (final IOException exception) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE, "Loading fxml for the synthesis transition node failed.", exception);
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    inputStateNode = synthesisContextService.getStateNodeFactory()
        .create(inputStateProperty.get(), traceProperty().get(), new Point2D(0, 0),
            nodeStateProperty().get());

    isExpandedProperty().bindBidirectional(inputStateNode.isExpandedProperty());

    inputStateNode.titleProperty().set("Input");
    inputStateNode.nodeWidthProperty().set(100);
    inputStateNode.nodeHeightProperty().set(100);
    inputStateNode.moveIsEnabledProperty().set(false);
    inputStateNode.stateProperty().bindBidirectional(inputStateProperty);

    outputStateNode = synthesisContextService.getStateNodeFactory()
        .create(outputStateProperty.get(), traceProperty().get(),
            new Point2D(inputStateNode.widthProperty().add(100.0).get(), 0),
            nodeStateProperty().get());
    outputStateNode.titleProperty().set("Output");
    outputStateNode.nodeWidthProperty().set(100);
    outputStateNode.nodeHeightProperty().set(100);
    outputStateNode.moveIsEnabledProperty().set(false);
    outputStateNode.stateProperty().bindBidirectional(outputStateProperty);
    outputStateNode.positionXProperty().bind(inputStateNode.widthProperty().add(100.0));

    inputStateNode.isExpandedProperty().bindBidirectional(outputStateNode.isExpandedProperty());

    prefWidthProperty().bind(
        inputStateNode.widthProperty().add(outputStateNode.widthProperty()).add(100.0));
    prefHeightProperty().bind(inputStateNode.heightProperty());

    getChildren().addAll(inputStateNode, outputStateNode);
    StackPane.setAlignment(inputStateNode, Pos.CENTER_LEFT);
    StackPane.setAlignment(outputStateNode, Pos.CENTER_RIGHT);
  }

  public State getInputState() {
    return inputStateProperty.get();
  }

  public ObjectProperty<State> getInputStateProperty() {
    return inputStateProperty;
  }

  public void setInputState(final State inputStateProperty) {
    this.inputStateProperty.set(inputStateProperty);
  }

  public State getOutputState() {
    return outputStateProperty.get();
  }

  public ObjectProperty<State> getOutputStateProperty() {
    return outputStateProperty;
  }

  public void setOutputState(final State outputStateProperty) {
    this.outputStateProperty.set(outputStateProperty);
  }

  public void childrenToFront() {
    inputStateNode.toFront();
    outputStateNode.toFront();
  }
}