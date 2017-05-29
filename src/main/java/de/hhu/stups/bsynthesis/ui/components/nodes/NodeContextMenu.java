package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.ui.Loader;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.net.URL;
import java.util.ResourceBundle;

public class NodeContextMenu extends ContextMenu implements Initializable {

  private final BasicNode parent;

  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemResizeNode;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemRemoveNode;

  /**
   * Load the fxml resource.
   */
  @Inject
  public NodeContextMenu(final FXMLLoader loader,
                         @Assisted BasicNode parent) {
    this.parent = parent;

    Loader.loadFxml(loader, this, "node_context_menu.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    menuItemResizeNode.textProperty().bind(Bindings.when(parent.isExpandedProperty())
        .then("Shrink node").otherwise("Expand node"));
    menuItemResizeNode.setOnAction(event -> parent.isExpandedProperty().set(!parent.isExpanded()));
    menuItemRemoveNode.setOnAction(event -> parent.remove());
  }
}
