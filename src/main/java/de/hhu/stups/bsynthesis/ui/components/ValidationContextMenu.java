package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * A context menu displaying several commands that can be applied on a {@link ValidationPane}.
 */
public class ValidationContextMenu extends ContextMenu {

  // graphicPositionProperty to place a node on the validation pane according to the mouse position
  // which can be different to the context menu's screen position when the pane is scaled
  private final ObjectProperty<Point2D> graphicPositionProperty;
  private final ObjectProperty<SynthesisType> synthesisTypeProperty;
  private final SynthesisContextService synthesisContextService;
  private final UiService uiService;

  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemAddNode;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemExpandAll;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemShrinkAll;

  /**
   * Initialize properties and layout.
   */
  @Inject
  public ValidationContextMenu(final FXMLLoader loader,
                               final ServiceDelegator serviceDelegator,
                               @Assisted final SynthesisType synthesisType) {
    this.synthesisTypeProperty = new SimpleObjectProperty<>(synthesisType);
    synthesisContextService = serviceDelegator.synthesisContextService();
    uiService = serviceDelegator.uiService();
    graphicPositionProperty = new SimpleObjectProperty<>(new Point2D(0.0, 0.0));

    Loader.loadFxml(loader, this, "validation_context_menu.fxml");
  }

  /**
   * Set the {@link ValidationPane} that this component is connected to.
   */
  public void setValidationPane(final ValidationPane validationPane) {
    menuItemAddNode.textProperty().bind(Bindings.when(synthesisTypeProperty
        .isEqualTo(SynthesisType.ACTION))
        .then("Add transition").otherwise("Add state"));

    menuItemAddNode.setOnAction(event -> {
      if (SynthesisType.ACTION.equals(synthesisTypeProperty.get())) {
        validationPane.addNode(uiService.getTransitionNodeFactory().create(null, null,
            new Point2D(graphicPositionProperty.get().getX(), graphicPositionProperty.get().getY()),
            NodeState.TENTATIVE));
      } else {
        validationPane.addNode(uiService.getStateNodeFactory().create(null, null,
            new Point2D(graphicPositionProperty.get().getX(), graphicPositionProperty.get().getY()),
            NodeState.TENTATIVE));
      }
    });

    menuItemExpandAll.setOnAction(event -> validationPane.expandAllNodes());
    menuItemShrinkAll.setOnAction(event -> validationPane.shrinkAllNodes());

    synthesisTypeProperty.bind(synthesisContextService.synthesisTypeProperty());
  }

  public ObjectProperty<Point2D> getGraphicPositionProperty() {
    return graphicPositionProperty;
  }
}
