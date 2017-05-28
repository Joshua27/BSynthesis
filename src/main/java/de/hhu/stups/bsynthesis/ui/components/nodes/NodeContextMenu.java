package de.hhu.stups.bsynthesis.ui.components.nodes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    loader.setLocation(getClass().getResource("node_context_menu.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (final IOException exception) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE, "Loading fxml for the synthesis context menu failed.", exception);
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    menuItemResizeNode.textProperty().bind(Bindings.when(parent.isExpandedProperty())
        .then("Shrink node").otherwise("Expand node"));
    menuItemResizeNode.setOnAction(event -> parent.isExpandedProperty().set(!parent.isExpanded()));
    menuItemRemoveNode.setOnAction(event -> parent.remove());
  }
}
