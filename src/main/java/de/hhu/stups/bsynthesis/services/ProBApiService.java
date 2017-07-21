package de.hhu.stups.bsynthesis.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.IntStream;

@Singleton
public class ProBApiService {

  private static final int ADDITIONAL_INSTANCES = 4;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ObjectProperty<StateSpace> mainStateSpaceProperty;
  private final SetProperty<StateSpace> stateSpacesProperty;
  private final Api proBApi;

  /**
   * Initialize properties and the injected {@link Api}.
   */
  @Inject
  public ProBApiService(final Api proBApi) {
    this.proBApi = proBApi;
    mainStateSpaceProperty = new SimpleObjectProperty<>();
    stateSpacesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
  }

  /**
   * Show a {@link FileChooser dialog} and open a machine from file.
   */
  public SpecificationType loadMachine(final Stage stage) {
    final FileChooser fileChooser = new FileChooser();
    final FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter("Machine (*.mch, *.eventb)", "*.mch", "*.eventb");
    fileChooser.getExtensionFilters().add(extFilter);
    final File file = fileChooser.showOpenDialog(stage);
    if (file == null) {
      return null;
    }
    final StateSpace stateSpace = loadStateSpace(file);
    if (stateSpace == null) {
      return null;
    }
    mainStateSpaceProperty.set(stateSpace);
    stateSpacesProperty.clear();
    // load the same model to several instances in a background thread
    new Thread(() ->
        IntStream.range(0, ADDITIONAL_INSTANCES).forEach(value -> {
          final StateSpace newStateSpace = loadStateSpace(file);
          if (newStateSpace != null) {
            stateSpacesProperty.add(newStateSpace);
          }
        })).start();
    return hasClassicalBExtension(file) ? SpecificationType.CLASSICAL_B : SpecificationType.EVENT_B;
  }

  private StateSpace loadStateSpace(final File file) {
    try {
      if (hasClassicalBExtension(file)) {
        return proBApi.b_load(file.getPath());
      } else {
        // TODO: fix this, getModelFile() returns null for Event-B machines?
        return proBApi.eventb_load(file.getPath());
      }
    } catch (final IOException exception) {
      logger.error("IOException while loading " + file.getPath(), exception);
    } catch (final ModelTranslationError modelTranslationError) {
      logger.error("Translation error while loading " + file.getPath(),
          modelTranslationError);
    }
    return null;
  }

  private boolean hasClassicalBExtension(final File file) {
    return ".mch".equals(file.getName().substring(file.getName().lastIndexOf('.')));
  }

  public ObjectProperty<StateSpace> mainStateSpaceProperty() {
    return mainStateSpaceProperty;
  }

  /**
   * Return the {@link #mainStateSpaceProperty main statespace} if not busy. Otherwise, return the
   * first idle statespace we can find in the additional {@link #stateSpacesProperty instances}. If
   * all statespaces are busy return null.
   */
  public StateSpace getIdleStateSpace() {
    final StateSpace mainStateSpace = mainStateSpaceProperty.get();
    if (!mainStateSpace.isBusy()) {
      return mainStateSpace;
    }
    final Optional<StateSpace> optionalStateSpace =
        stateSpacesProperty.get().stream().filter(stateSpace -> !stateSpace.isBusy()).findFirst();
    return optionalStateSpace.orElse(null);
  }

  /**
   * Synchronize the {@link #stateSpacesProperty} with the {@link #mainStateSpaceProperty}.
   */
  public void synchronizeStateSpaces() {
    stateSpacesProperty.clear();
    IntStream.range(0, ADDITIONAL_INSTANCES).forEach(value ->
        stateSpacesProperty.add(mainStateSpaceProperty.get()));
    logger.info("Synchronized statespaces in the ProBApiService used for synthesis.");
  }

  public StateSpace getMainStateSpace() {
    return mainStateSpaceProperty.get();
  }
}