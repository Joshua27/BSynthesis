package de.hhu.stups.bsynthesis.ui;

import javafx.fxml.FXMLLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
      final Logger logger = LoggerFactory.getLogger(root.getClass());
      logger.error("Loading fxml for " + root.toString() + " failed.", exception);
    }
  }

}
