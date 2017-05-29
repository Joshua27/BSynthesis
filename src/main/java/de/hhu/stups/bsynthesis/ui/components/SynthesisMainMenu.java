package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.StateSpace;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SynthesisMainMenu extends MenuBar implements Initializable {

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final Api proBApi;

  private final ObjectProperty<Stage> stageProperty;
  private final ValidationPane validationPane;
  private final SynthesisContextService synthesisContextService;
  private final SynthesisInfoBox synthesisInfoBox;
  private final ModelCheckingService modelCheckingService;

  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemSave;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemSaveAs;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemClear;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemNewOperation;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemVisualizeOperation;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemCheckModel;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemStopCheckModel;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemRunSynthesis;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemConfigureLibrary;
  @FXML
  @SuppressWarnings("unused")
  private CheckMenuItem checkMenuItemInfo;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemExpandAll;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemShrinkAll;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemNodesFromTrace;

  /**
   * Initialize the variables derived by the injector and load the fxml resource.
   */
  @Inject
  public SynthesisMainMenu(final FXMLLoader loader,
                           final Api proBApi,
                           final ValidationPane validationPane,
                           final SynthesisInfoBox synthesisInfoBox,
                           final SynthesisContextService synthesisContextService,
                           final ModelCheckingService modelCheckingService) {
    this.proBApi = proBApi;
    this.validationPane = validationPane;
    this.synthesisInfoBox = synthesisInfoBox;
    this.synthesisContextService = synthesisContextService;
    this.modelCheckingService = modelCheckingService;
    stageProperty = new SimpleObjectProperty<>();

    Loader.loadFxml(loader, this, "synthesis_main_menu.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    BooleanBinding disableMenu = synthesisContextService.stateSpaceProperty().isNull();
    menuItemClear.disableProperty().bind(disableMenu
        .or(modelCheckingService.indicatorPresentProperty()));
    menuItemSave.disableProperty().bind(disableMenu);
    menuItemSaveAs.disableProperty().bind(disableMenu);
    menuItemExpandAll.disableProperty().bind(disableMenu);
    menuItemShrinkAll.disableProperty().bind(disableMenu);
    menuItemCheckModel.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty())
        .or(modelCheckingService.indicatorPresentProperty())
        .or(modelCheckingService.resultProperty().isNotNull()));
    menuItemStopCheckModel.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty().not()));
    menuItemVisualizeOperation.disableProperty().bind(disableMenu
        .or(modelCheckingService.errorFoundProperty().isNotNull())
        .or(modelCheckingService.indicatorPresentProperty()));
    menuItemNewOperation.disableProperty().bind(disableMenu
        .or(modelCheckingService.errorFoundProperty().isNotNull())
        .or(modelCheckingService.indicatorPresentProperty()));
    menuItemNodesFromTrace.disableProperty().bind(disableMenu
        .or(modelCheckingService.errorFoundProperty().isNull())
        .or(modelCheckingService.indicatorPresentProperty()));
    checkMenuItemInfo.selectedProperty().bindBidirectional(synthesisInfoBox.showInfoProperty());
    checkMenuItemInfo.disableProperty().bind(disableMenu);
    // TODO: run synthesis menu item disabling
    menuItemRunSynthesis.disableProperty().bind(new SimpleBooleanProperty(true)
        .or(modelCheckingService.indicatorPresentProperty()));
  }

  /**
   * Load a machine from a .mch file.
   */
  @FXML
  @SuppressWarnings("unused")
  public void loadMachine() {
    modelCheckingService.reset();
    final FileChooser fileChooser = new FileChooser();
    final FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter("Machine (*.mch)", "*.mch");
    fileChooser.getExtensionFilters().add(extFilter);
    final File file = fileChooser.showOpenDialog(stageProperty.get());
    try {
      if (file == null) {
        return;
      }
      synthesisContextService.setStateSpace(proBApi.b_load(file.getPath()));
      synthesisContextService.showSynthesisTabProperty().set(true);
    } catch (final IOException exception) {
      logger.log(Level.SEVERE, "IOException while loading " + file.getPath(), exception);
    } catch (final ModelTranslationError modelTranslationError) {
      logger.log(Level.SEVERE, "Translation error while loading " + file.getPath(),
          modelTranslationError);
    }
  }

  /**
   * Create a new operation using synthesis. Therefore, clear the {@link ValidationPane} and set
   * specific context properties.
   */
  @FXML
  @SuppressWarnings("unused")
  public void newOperation() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    validationPane.getNodes().clear();
    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    synthesisContextService.setCurrentOperation("none");
    synthesisInfoBox.reset();
  }

  /**
   * Clear the {@link ValidationPane} and show the nodes from the trace found by the model checker.
   * Only enabled if the model checker found an error {@link
   * ModelCheckingService#errorFoundProperty()}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void showNodesFromTrace() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    synthesisContextService.setSynthesisType(SynthesisType.GUARD_OR_INVARIANT);
    validationPane.getNodes().clear();
    validationPane.initializeNodesFromTrace();
  }

  /**
   * Visualize an existing operation by collecting several transitions.
   */
  @FXML
  @SuppressWarnings("unused")
  public void visualizeOperation() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    validationPane.getNodes().clear();
    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    // TODO
  }

  /**
   * Run the model checker and display the progress using the {@link
   * ModelCheckingProgressIndicator}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void runModelChecking() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    validationPane.reset();
    if (stateSpace == null) {
      return;
    }
    modelCheckingService.stateSpaceProperty().set(null);
    modelCheckingService.stateSpaceProperty()
        .set(synthesisContextService.stateSpaceProperty().get());
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void stopModelChecking() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    modelCheckingService.runningProperty().set(false);
    modelCheckingService.indicatorPresentProperty().set(false);
  }

  @FXML
  @SuppressWarnings("unused")
  public void save() {
    // TODO
  }

  @FXML
  @SuppressWarnings("unused")
  public void saveAs() {
    // TODO
  }

  @FXML
  @SuppressWarnings("unused")
  public void runSynthesis() {
    // TODO
  }

  /**
   * Clear the {@link ValidationPane}, i.e., delete all available nodes.
   */
  @FXML
  @SuppressWarnings("unused")
  public void clear() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    synthesisContextService.synthesisTypeProperty().set(SynthesisType.ACTION);
    synthesisContextService.currentOperationProperty().set(null);
    validationPane.getNodes().clear();
    synthesisInfoBox.isMinimizedProperty().set(true);
  }

  @FXML
  @SuppressWarnings("unused")
  public void close() {
    stageProperty().get().close();
  }

  @FXML
  @SuppressWarnings("unused")
  public void configureLibrary() {
    // TODO: fix this
    synthesisContextService.showLibraryConfigurationProperty().set(true);
  }

  @FXML
  @SuppressWarnings("unused")
  public void expandAllNodes() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    validationPane.expandAllNodes();
  }

  @FXML
  @SuppressWarnings("unused")
  public void shrinkAllNodes() {
    synthesisContextService.showSynthesisTabProperty().set(true);
    validationPane.shrinkAllNodes();
  }

  public ObjectProperty<Stage> stageProperty() {
    return stageProperty;
  }
}
