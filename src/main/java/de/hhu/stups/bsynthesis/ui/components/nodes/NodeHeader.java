package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class NodeHeader extends HBox implements Initializable {

  private final ObjectProperty<StateNode> stateNodeProperty;
  private final SynthesisContextService synthesisContextService;

  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconFindPredecessor;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconFindSuccessor;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconValidate;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconExpand;

  /**
   * Initialize properties and load the fxml resource.
   */
  @Inject
  public NodeHeader(final FXMLLoader loader,
                    final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;
    stateNodeProperty = new SimpleObjectProperty<>();

    Loader.loadFxml(loader, this, "node_header.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    stateNodeProperty.addListener((observable, oldValue, newValue) -> initializeIcons());
  }

  private void initializeIcons() {
    setIconExistence();
    final StateNode stateNode = stateNodeProperty.get();
    stateNode.nodeStateProperty().addListener((observable, oldValue, newValue) ->
        setIconExistence());

    final ObservableValue<Number> iconBinding =
        Bindings.when(stateNode.isExpandedProperty()).then(18).otherwise(15);
    iconExpand.glyphNameProperty().bind(
        Bindings.when(stateNode.isExpandedProperty()).then("COMPRESS").otherwise("EXPAND"));
    iconExpand.glyphSizeProperty().bind(iconBinding);
    iconExpand.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (MouseButton.SECONDARY.equals(event.getButton())) {
        return;
      }
      stateNode.isExpandedProperty().set(!stateNode.isExpanded());
    });

    final BooleanBinding traceWalkingEnabledBinding = stateNode.nodeStateProperty()
        .isNotEqualTo(NodeState.TENTATIVE)
        .and(synthesisContextService.synthesisTypeProperty()
            .isEqualTo(SynthesisType.GUARD_OR_INVARIANT))
        .and(stateNode.stateFromModelCheckingProperty());

    final BooleanBinding canGoBackBinding = Bindings.createBooleanBinding(
        stateNode.traceProperty().get() != null
            ? stateNode.traceProperty().get()::canGoBack : () -> false);
    iconFindPredecessor.glyphStyleProperty().bind(
        Bindings.when(stateNode.predecessorProperty().emptyProperty().and(
            canGoBackBinding)).then("-fx-fill: #000000;").otherwise("-fx-fill: #747474;"));
    iconFindPredecessor.visibleProperty().bind(traceWalkingEnabledBinding);
    iconFindPredecessor.disableProperty().bind(synthesisContextService.synthesisTypeProperty()
        .isEqualTo(SynthesisType.ACTION).or(canGoBackBinding.not()));
    iconFindPredecessor.glyphSizeProperty().bind(iconBinding);
    iconFindPredecessor.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (MouseButton.SECONDARY.equals(event.getButton())
          || !stateNode.predecessorProperty().isEmpty()) {
        return;
      }
      stateNode.findPredecessor();
    });

    final BooleanBinding canGoForwardBinding = Bindings.createBooleanBinding(
        stateNode.traceProperty().get() != null
            ? stateNode.traceProperty().get()::canGoForward : () -> false);
    iconFindSuccessor.glyphStyleProperty().bind(
        Bindings.when(stateNode.successorProperty().emptyProperty().and(
            canGoForwardBinding)).then("-fx-fill: #000000;").otherwise("-fx-fill: #747474;"));
    iconFindSuccessor.visibleProperty().bind(traceWalkingEnabledBinding);
    iconFindSuccessor.disableProperty().bind(synthesisContextService.synthesisTypeProperty()
        .isEqualTo(SynthesisType.ACTION).or(canGoForwardBinding.not()));
    iconFindSuccessor.glyphSizeProperty().bind(iconBinding);
    iconFindSuccessor.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (MouseButton.SECONDARY.equals(event.getButton())
          || !stateNode.successorProperty().isEmpty()) {
        return;
      }
      stateNode.findSuccessor();
    });

    iconValidate.visibleProperty().bind(stateNode.nodeStateProperty()
        .isEqualTo(NodeState.TENTATIVE));
    iconValidate.disableProperty().bind(stateNode.nodeStateProperty()
        .isNotEqualTo(NodeState.TENTATIVE));
    iconValidate.glyphSizeProperty().bind(iconBinding);
    iconValidate.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
      if (MouseButton.SECONDARY.equals(event.getButton())) {
        return;
      }
      stateNode.validateState();
    });
  }

  private void setIconExistence() {
    if (!NodeState.TENTATIVE.equals(stateNodeProperty.get().getNodeState())) {
      getChildren().remove(iconValidate);
      return;
    }
    if (getChildren().contains(iconValidate)) {
      return;
    }
    getChildren().add(2, iconValidate);
  }

  void setBasicNode(final StateNode stateNode) {
    stateNodeProperty.set(stateNode);
  }
}
