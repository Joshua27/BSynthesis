package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.prob.DistinguishingExample;
import de.hhu.stups.bsynthesis.prob.SetSolverTimeoutCommand;
import de.hhu.stups.bsynthesis.prob.StartSynthesisCommand;
import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.hhu.stups.bsynthesis.ui.controller.ControllerTab;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.animator.command.FindValidStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class SynthesisMainMenu extends MenuBar implements Initializable {

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Api proBApi;
  private final ObjectProperty<Stage> stageProperty;
  private final ValidationPane validationPane;
  private final SynthesisContextService synthesisContextService;
  private final SynthesisInfoBox synthesisInfoBox;
  private final ModelCheckingService modelCheckingService;
  private final BooleanProperty synthesisRunningProperty;
  private final ObjectProperty<Task<Boolean>> runSynthesisTaskProperty;
  private final BooleanProperty ignoreModelCheckerProperty;
  private final UiService uiService;

  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemOpen;
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
  private MenuItem menuItemStopSynthesis;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemSetTimeout;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemConfigureLibrary;
  @FXML
  @SuppressWarnings("unused")
  private CheckMenuItem checkMenuItemInfo;
  @FXML
  @SuppressWarnings("unused")
  private CheckMenuItem checkMenuItemIgnoreChecker;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemExpandAll;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemShrinkAll;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemVerifyAllNodes;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemNodesFromTrace;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemZoomIn;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem menuItemZoomOut;

  /**
   * Initialize the variables derived by the injector and load the fxml resource.
   */
  @Inject
  public SynthesisMainMenu(final FXMLLoader loader,
                           final Api proBApi,
                           final ValidationPane validationPane,
                           final SynthesisInfoBox synthesisInfoBox,
                           final SynthesisContextService synthesisContextService,
                           final ModelCheckingService modelCheckingService,
                           final UiService uiService) {
    this.proBApi = proBApi;
    this.validationPane = validationPane;
    this.synthesisInfoBox = synthesisInfoBox;
    this.synthesisContextService = synthesisContextService;
    this.modelCheckingService = modelCheckingService;
    this.uiService = uiService;
    stageProperty = new SimpleObjectProperty<>();
    synthesisRunningProperty = new SimpleBooleanProperty(false);
    runSynthesisTaskProperty = new SimpleObjectProperty<>();
    ignoreModelCheckerProperty = new SimpleBooleanProperty(false);

    Loader.loadFxml(loader, this, "synthesis_main_menu.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    initializeMenuItemBindings();
  }

  private void initializeMenuItemBindings() {
    final BooleanBinding disableMenu = synthesisContextService.stateSpaceProperty().isNull()
        .or(synthesisRunningProperty)
        .or(synthesisContextService.synthesisSucceededProperty());
    final BooleanBinding extendMachineDisabled = disableMenu
        .or(Bindings.when(ignoreModelCheckerProperty).then(false)
            .otherwise(modelCheckingService.errorFoundProperty().isNotNull()
                .or(modelCheckingService.indicatorPresentProperty())
                .or(modelCheckingService.resultProperty().isNull())));
    menuItemOpen.disableProperty().bind(synthesisContextService.synthesisSucceededProperty());
    menuItemClear.disableProperty().bind(disableMenu
        .or(modelCheckingService.indicatorPresentProperty()));
    menuItemSaveAs.disableProperty().bind(disableMenu);
    menuItemExpandAll.disableProperty().bind(disableMenu);
    menuItemShrinkAll.disableProperty().bind(disableMenu);
    menuItemCheckModel.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty())
        .or(modelCheckingService.indicatorPresentProperty())
        .or(ignoreModelCheckerProperty)
        .or(modelCheckingService.resultProperty().isNotNull()));
    menuItemStopCheckModel.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty().not()));
    menuItemVisualizeOperation.disableProperty().bind(extendMachineDisabled);
    menuItemNewOperation.disableProperty().bind(extendMachineDisabled);
    menuItemNodesFromTrace.disableProperty().bind(disableMenu
        .or(ignoreModelCheckerProperty)
        .or(modelCheckingService.errorFoundProperty().isNull())
        .or(modelCheckingService.indicatorPresentProperty()));
    checkMenuItemInfo.selectedProperty().bindBidirectional(synthesisInfoBox.showInfoProperty());
    checkMenuItemInfo.disableProperty().bind(disableMenu
        .or(synthesisContextService.synthesisTypeProperty().isEqualTo(SynthesisType.NONE)));
    checkMenuItemIgnoreChecker.selectedProperty().bindBidirectional(ignoreModelCheckerProperty);
    checkMenuItemIgnoreChecker.disableProperty().bind(disableMenu
        .or(modelCheckingService.indicatorPresentProperty())
        .or(modelCheckingService.runningProperty()));
    menuItemVerifyAllNodes.disableProperty().bind(disableMenu
        .or(synthesisRunningProperty)
        .or(modelCheckingService.indicatorPresentProperty())
        .or(validationPane.getNodes().emptyProperty()));
    menuItemRunSynthesis.disableProperty().bind(disableMenu
        .or(validationPane.getNodes().emptyProperty())
        .or(modelCheckingService.indicatorPresentProperty())
        .or(synthesisRunningProperty));
    menuItemStopSynthesis.disableProperty().bind(disableMenu
        .or(validationPane.getNodes().emptyProperty())
        .or(modelCheckingService.indicatorPresentProperty())
        .or(synthesisRunningProperty.not()));
    menuItemConfigureLibrary.disableProperty()
        .bind(synthesisContextService.synthesisSucceededProperty());
    menuItemZoomIn.disableProperty().bind(uiService.zoomInEnabledProperty().not());
    menuItemZoomOut.disableProperty().bind(uiService.zoomOutEnabledProperty().not());
    synthesisContextService.contextEventStream().subscribe(contextEvent -> {
      if (ContextEvent.RESET_CONTEXT.equals(contextEvent)) {
        modelCheckingService.reset();
      }
    });
  }

  /**
   * Load a machine from a .mch file.
   */
  @FXML
  @SuppressWarnings("unused")
  public void loadMachine() {
    synthesisContextService.contextEventStream().push(ContextEvent.RESET_CONTEXT);
    Platform.runLater(getOpenFileRunnable());
  }

  /**
   * Create a new operation using synthesis. Clear the {@link ValidationPane} and set specific
   * context properties. The user is asked to specify a name for the operation to be synthesized.
   */
  @FXML
  @SuppressWarnings("unused")
  public void newOperation() {
    final Optional<String> operationNameOptional = getOperationNameFromDialog();
    if (!operationNameOptional.isPresent()) {
      return;
    }
    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    synthesisContextService.setCurrentOperation(operationNameOptional.get());
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    validationPane.getNodes().clear();
    synthesisInfoBox.reset();
    synthesisInfoBox.showInfoProperty().set(true);
  }

  /**
   * Clear the {@link ValidationPane} and show the nodes from the trace found by the model checker.
   * Only enabled if the model checker found an error {@link
   * ModelCheckingService#errorFoundProperty()}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void showNodesFromTrace() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    synthesisContextService.setSynthesisType(SynthesisType.GUARD);
    validationPane.getNodes().clear();
    validationPane.initializeNodesFromTrace();
  }

  /**
   * Visualize an existing operation by collecting several transitions.
   */
  @FXML
  @SuppressWarnings("unused")
  public void visualizeOperation() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
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
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    validationPane.reset();
    if (stateSpace == null) {
      return;
    }
    modelCheckingService.stateSpaceEventStream().push(stateSpace);
  }

  /**
   * Show a dialog to set the ProB solver timeout using {@link SetSolverTimeoutCommand}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void setTimeout() {
    final Optional<String> timeoutOptional = getTimeoutFromDialog();
    timeoutOptional.ifPresent(timeout -> {
      final SetSolverTimeoutCommand setSolverTimeoutCommand =
          new SetSolverTimeoutCommand(new BigInteger(timeout));
      synthesisContextService.getStateSpace().execute(setSolverTimeoutCommand);
    });
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void stopModelChecking() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    modelCheckingService.runningProperty().set(false);
    modelCheckingService.indicatorPresentProperty().set(false);
  }

  @FXML
  @SuppressWarnings("unused")
  public void saveAs() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
  }

  /**
   * Validate all state nodes by checking the current machine invariants on each state.
   */
  @FXML
  @SuppressWarnings("unused")
  public void verifyAllNodes() {
    validationPane.getNodes().forEach(basicNode -> {
      if (basicNode instanceof TransitionNode) {
        final TransitionNode transitionNode = (TransitionNode) basicNode;
        transitionNode.validateTransition();
        return;
      }
      ((StateNode) basicNode).validateState();
    });
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void zoomIn() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    uiService.zoomEventStream().push(UiService.UiZoom.ZOOM_IN);
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void zoomOut() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    uiService.zoomEventStream().push(UiService.UiZoom.ZOOM_OUT);
  }

  /**
   * Start the synthesis prolog backend by executing {@link StartSynthesisCommand}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void runSynthesis() {
    if (synthesisRunningProperty.get()) {
      return;
    }
    final Task<Boolean> runSynthesisTask = getRunSynthesisTask();
    if (runSynthesisTask == null) {
      return;
    }
    runSynthesisTaskProperty.set(runSynthesisTask);
    runSynthesisTask.setOnSucceeded(event -> {
      try {
        synthesisContextService.synthesisSucceededProperty().set(runSynthesisTask.get());
      } catch (final InterruptedException interruptedException) {
        logger.error("Synthesis task interrupted during execution.", interruptedException);
        Thread.currentThread().interrupt();
      } catch (final ExecutionException executionException) {
        logger.error("Exception during excecution of the synthesis task.", executionException);
      }
    });
    runSynthesisTask.setOnFailed(event -> System.out.println("synthesis failed"));
    synthesisRunningProperty.unbind();
    synthesisRunningProperty.bind(runSynthesisTask.runningProperty());
    EXECUTOR_SERVICE.execute(runSynthesisTask);
  }

  private void shutdownExecutor(final ExecutorService executorService) {
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException ie) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Stop synthesis.
   */
  @FXML
  @SuppressWarnings("unused")
  public void stopSynthesis() {
    final Task<Boolean> runSynthesisTask = runSynthesisTaskProperty.get();
    if (synthesisRunningProperty.get() && runSynthesisTask != null) {
      runSynthesisTask.cancel(true);
      synthesisContextService.stateSpaceProperty().get().sendInterrupt();
    }
  }

  /**
   * Create a task that calls the synthesis prolog backend using {@link StartSynthesisCommand}.
   */
  private Task<Boolean> getRunSynthesisTask() {
    final List<BasicNode> invalidNodes = validationPane.getInvalidNodes();
    addPredecessorNodesIfGuard(invalidNodes);
    final List<BasicNode> validNodes = validationPane.getValidNodes();
    if (validNodes.isEmpty() && invalidNodes.isEmpty()) {
      return null;
    }
    return new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        final StartSynthesisCommand startSynthesisCommand = new StartSynthesisCommand(
            synthesisContextService.selectedLibraryComponentsProperty().get(),
            synthesisContextService.getCurrentOperation(),
            synthesisContextService.synthesisTypeProperty().get(),
            validNodes, invalidNodes);
        final StateSpace stateSpace = synthesisContextService.stateSpaceProperty().get();
        startSynthesisCommand.distinguishingExampleProperty()
            .addListener((observable, oldValue, newValue) -> handleDistinguishingExample(newValue));
        stateSpace.execute(startSynthesisCommand);
        synthesisContextService.modifiedMachineCodeProperty().set(
            startSynthesisCommand.modifiedMachineCodeProperty().get());
        return startSynthesisCommand.synthesisSucceededProperty().get();
      }
    };
  }

  private void handleDistinguishingExample(final DistinguishingExample distinguishingExample) {
    final Point2D distinguishingNodePosition =
        new Point2D(ValidationPane.WIDTH / 2, ValidationPane.HEIGHT / 2);
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final FindValidStateCommand inputStateCommand = new FindValidStateCommand(
        stateSpace, new ClassicalB(distinguishingExample.getInputStateEquality()));
    stateSpace.execute(inputStateCommand);
    System.out.println("dist in");
    System.out.println(inputStateCommand);
    System.out.println(inputStateCommand.getStateId());
    final State inputState = stateSpace.getState(inputStateCommand.getStateId());

    if (!distinguishingExample.getOutputTuples().isEmpty()) {
      final FindValidStateCommand outputStateCommand = new FindValidStateCommand(
          stateSpace, new ClassicalB(distinguishingExample.getOutputStateEquality()));
      stateSpace.execute(outputStateCommand);

      // TODO: problem with states that violate the invariant, waiting for FindStateCommand

      System.out.println("dist out");
      System.out.println(outputStateCommand);
      System.out.println(outputStateCommand.getStateId());
      final State outputState = stateSpace.getState(outputStateCommand.getStateId());
      final TransitionNode transitionNode = uiService.getTransitionNodeFactory()
          .create(inputState, outputState, distinguishingNodePosition, NodeState.TENTATIVE);
      Platform.runLater(() -> {
        transitionNode.isExpandedProperty().set(true);
        transitionNode.validateTransition();
      });
      validationPane.addNode(transitionNode);
      return;
    }

    if (synthesisContextService.synthesisTypeProperty().get().isAction()) {
      // synthesizing an action but simultaneous synthesis of an appropriate guard returned a
      // distinguishing state, therefore we set the transitions input and output state to be equal:
      // if the state should be negative we only need the input, otherwise the user has to adapt
      // the output anyways
      final TransitionNode transitionNode = uiService.getTransitionNodeFactory()
          .create(inputState, inputState, distinguishingNodePosition, NodeState.TENTATIVE);
      validationPane.addNode(transitionNode);
      Platform.runLater(() -> {
        transitionNode.isExpandedProperty().set(true);
        transitionNode.validateTransition();
      });
      return;
    }

    final StateNode stateNode = uiService.getStateNodeFactory()
        .create(inputState, stateSpace.getTrace(inputState.getId()), distinguishingNodePosition,
            NodeState.TENTATIVE);
    Platform.runLater(() -> {
      stateNode.isExpandedProperty().set(true);
      stateNode.validateState();
    });
    validationPane.addNode(stateNode);
  }

  /**
   * Add the predecessor states of the negative states to the negative examples when strengthening a
   * guard. We wan't to block the state leading to the violating state and thus need the
   * predecessor.
   */
  private void addPredecessorNodesIfGuard(final List<BasicNode> invalidNodes) {
    if (SynthesisType.GUARD.equals(synthesisContextService.synthesisTypeProperty().get())) {
      final List<StateNode> predecessorNodes = new ArrayList<>();
      invalidNodes.forEach(basicNode -> ((StateNode) basicNode).predecessorProperty().get()
          .forEach(stateNode -> predecessorNodes.add((StateNode) stateNode)));
      invalidNodes.addAll(predecessorNodes);
    }
  }

  /**
   * Clear the {@link ValidationPane}, i.e., delete all available nodes.
   */
  @FXML
  @SuppressWarnings("unused")
  public void clear() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    synthesisContextService.synthesisTypeProperty().set(SynthesisType.NONE);
    synthesisContextService.currentOperationProperty().set(null);
    validationPane.getNodes().clear();
    synthesisInfoBox.isMinimizedProperty().set(true);
  }

  @FXML
  @SuppressWarnings("unused")
  public void close() {
    stageProperty().get().close();
  }

  /**
   * Open the library configuration tab.
   */
  @FXML
  @SuppressWarnings("unused")
  public void configureLibrary() {
    uiService.showTabEventStream().push(ControllerTab.LIBRARY_CONFIGURATION);
  }

  /**
   * Expand all nodes on the {@link ValidationPane}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void expandAllNodes() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    validationPane.expandAllNodes();
  }

  /**
   * Shrink all nodes on the {@link ValidationPane}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void shrinkAllNodes() {
    uiService.showTabEventStream().push(ControllerTab.SYNTHESIS);
    validationPane.shrinkAllNodes();
  }

  public ObjectProperty<Stage> stageProperty() {
    return stageProperty;
  }

  /**
   * Show a {@link TextInputDialog} and ask the user for a name for the new operation.
   */
  private Optional<String> getOperationNameFromDialog() {
    final TextInputDialog textInputDialog =
        getTextInputDialog("New Operation", "Set a name for the new operation:");
    final Optional<String> operationNameOptional = textInputDialog.showAndWait();
    if (operationNameOptional.isPresent() && !isValidOperationName(operationNameOptional.get())) {
      return getOperationNameFromDialog();
    }
    return operationNameOptional;
  }

  /**
   * Show a {@link TextInputDialog} to set the ProB solver timeout.
   */
  private Optional<String> getTimeoutFromDialog() {
    final TextInputDialog textInputDialog =
        getTextInputDialog("New Timeout", "ProB timeout in milliseconds:");
    final Optional<String> timeoutOptional = textInputDialog.showAndWait();
    if (timeoutOptional.isPresent() && (!NumberUtils.isNumber(timeoutOptional.get())
        || Double.valueOf(timeoutOptional.get()).intValue() <= 0)) {
      return getTimeoutFromDialog();
    }
    return timeoutOptional;
  }

  private TextInputDialog getTextInputDialog(final String title,
                                             final String contentText) {
    final TextInputDialog textInputDialog = new TextInputDialog();
    textInputDialog.setTitle(title);
    textInputDialog.setHeaderText(null);
    textInputDialog.setContentText(contentText);
    return textInputDialog;
  }

  /**
   * An operation name is valid if it doesn't already exist and does not contain any special
   * character except of underscore '_'.
   */
  private boolean isValidOperationName(final String operationName) {
    // TODO: check if operation with this name already exists
    final Pattern p = Pattern.compile("[^a-z0-9_ ]", Pattern.CASE_INSENSITIVE);
    return operationName.split(" ").length == 1
        && !p.matcher(operationName).find();
  }

  private Runnable getOpenFileRunnable() {
    return () -> {
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
        uiService.showTabEventStream().push(ControllerTab.CODEVIEW);
      } catch (final IOException exception) {
        logger.error("IOException while loading " + file.getPath(), exception);
      } catch (final ModelTranslationError modelTranslationError) {
        logger.error("Translation error while loading " + file.getPath(), modelTranslationError);
      }
    };
  }
}
