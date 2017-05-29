package de.hhu.stups.bsynthesis.ui;

import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface Loader {

  /**
   * Load an fxml file for a given {@link FXMLLoader} and root object.
   */
  static void loadFxml(final FXMLLoader loader,
                       final Object root,
                       final String fxmlFileName) {

    loader.setLocation(root.getClass().getResource(fxmlFileName));
    loader.setRoot(root);
    loader.setController(root);
    try {
      loader.load();
    } catch (final IOException exception) {
      final Logger logger = Logger.getLogger(root.getClass().getSimpleName());
      logger.log(Level.SEVERE, "Loading fxml for " + root.toString() + " failed.", exception);
    }
  }

}
