package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.prob.GetMachineOperationNamesCommand;
import de.hhu.stups.bsynthesis.prob.StartSynthesisCommand;
import de.hhu.stups.bsynthesis.services.ApplicationEvent;
import de.hhu.stups.bsynthesis.services.ApplicationEventType;
import de.hhu.stups.bsynthesis.services.ControllerTab;
import de.hhu.stups.bsynthesis.services.MachineVisualization;
import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.ProBApiService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SolverBackend;
import de.hhu.stups.bsynthesis.services.SpecificationType;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.services.UncoveredError;
import de.hhu.stups.bsynthesis.services.VisualizationType;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.animator.command.GetPreferenceCommand;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.statespace.StateSpace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.lang.math.NumberUtils;
import org.fxmisc.easybind.EasyBind;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class SynthesisMainMenu extends MenuBar implements Initializable {

  private final ObjectProperty<Stage> stageProperty;
  private final ValidationPane validationPane;
  private final SynthesisContextService synthesisContextService;
  private final SynthesisInfoBox synthesisInfoBox;
  private final ModelCheckingService modelCheckingService;
  private final BooleanProperty synthesisRunningProperty;
  private final BooleanProperty ignoreModelCheckerProperty;
  private final UiService uiService;
  private final ProBApiService proBApiService;

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
  private MenuItem menuItemModifyInvariants;
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
  @FXML
  @SuppressWarnings("unused")
  private MenuItem radioMenuItemProB;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem radioMenuItemZ3;

  /**
   * Initialize the variables derived by the injector and load the fxml resource.
   */
  @Inject
  public SynthesisMainMenu(final FXMLLoader loader,
                           final ValidationPane validationPane,
                           final SynthesisInfoBox synthesisInfoBox,
                           final ServiceDelegator serviceDelegator) {
    this.validationPane = validationPane;
    this.synthesisInfoBox = synthesisInfoBox;
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    this.modelCheckingService = serviceDelegator.modelCheckingService();
    this.uiService = serviceDelegator.uiService();
    this.proBApiService = serviceDelegator.proBApiService();
    stageProperty = new SimpleObjectProperty<>();
    synthesisRunningProperty = new SimpleBooleanProperty(false);
    ignoreModelCheckerProperty = new SimpleBooleanProperty(false);

    Loader.loadFxml(loader, this, "synthesis_main_menu.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    initializeMenuItemBindings();
    radioMenuItemProB.onActionProperty().addListener((observable, oldValue, newValue) ->
        synthesisContextService.solverBackendProperty().set(SolverBackend.PROB));
    radioMenuItemZ3.onActionProperty().addListener((observable, oldValue, newValue) ->
        synthesisContextService.solverBackendProperty().set(SolverBackend.Z3));
    // synchronize statespaces after model checking
    EasyBind.subscribe(modelCheckingService.resultProperty(), modelCheckingResult ->
        proBApiService.synchronizeStateSpaces());

    synthesisRunningProperty.bind(proBApiService.synthesisRunningProperty());
  }

  private void initializeMenuItemBindings() {
    final BooleanBinding disableMenu = synthesisContextService.stateSpaceProperty().isNull()
        .or(synthesisRunningProperty).or(modelCheckingService.indicatorPresentProperty());
    final BooleanBinding extendMachineDisabled = disableMenu
        .or(Bindings.when(ignoreModelCheckerProperty).then(false)
            .otherwise(modelCheckingService.errorTraceProperty().isNotNull()
                .or(modelCheckingService.resultProperty().isNull())
                .or(modelCheckingService.invariantViolationInitialState())));
    menuItemOpen.disableProperty().bind(synthesisContextService.synthesisSucceededProperty());
    menuItemClear.disableProperty().bind(disableMenu);
    menuItemSaveAs.disableProperty().bind(disableMenu);
    menuItemExpandAll.disableProperty().bind(disableMenu);
    menuItemShrinkAll.disableProperty().bind(disableMenu);
    menuItemCheckModel.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty())
        .or(ignoreModelCheckerProperty)
        .or(modelCheckingService.resultProperty().isNotNull()));
    menuItemStopCheckModel.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty().not()));
    menuItemVisualizeOperation.disableProperty().bind(extendMachineDisabled);
    menuItemNewOperation.disableProperty().bind(extendMachineDisabled);
    menuItemModifyInvariants.disableProperty().bind(extendMachineDisabled);
    menuItemNodesFromTrace.disableProperty().bind(disableMenu
        .or(ignoreModelCheckerProperty)
        .or(modelCheckingService.invariantViolationInitialState())
        .or(modelCheckingService.errorTraceProperty().isNull()));
    checkMenuItemInfo.selectedProperty().bindBidirectional(synthesisInfoBox.showInfoProperty());
    checkMenuItemInfo.disableProperty().bind(disableMenu
        .or(synthesisContextService.synthesisTypeProperty().isEqualTo(SynthesisType.NONE)));
    checkMenuItemIgnoreChecker.selectedProperty().bindBidirectional(ignoreModelCheckerProperty);
    checkMenuItemIgnoreChecker.disableProperty().bind(disableMenu
        .or(modelCheckingService.runningProperty()));
    menuItemVerifyAllNodes.disableProperty().bind(disableMenu
        .or(synthesisRunningProperty)
        .or(validationPane.getNodes().emptyProperty()));
    menuItemRunSynthesis.disableProperty().bind(disableMenu
        .or(validationPane.getNodes().emptyProperty())
        .or(synthesisRunningProperty));
    menuItemStopSynthesis.disableProperty().bind(synthesisRunningProperty.not());
    menuItemConfigureLibrary.disableProperty()
        .bind(synthesisContextService.synthesisSucceededProperty());
    menuItemZoomIn.disableProperty().bind(uiService.zoomInEnabledProperty().not());
    menuItemZoomOut.disableProperty().bind(uiService.zoomOutEnabledProperty().not());
    menuItemSetTimeout.disableProperty().bind(disableMenu);
  }

  /**
   * Load a machine from a .mch file.
   */
  @FXML
  @SuppressWarnings("unused")
  public void loadMachine() {
    final FileChooser fileChooser = new FileChooser();
    final FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter("Machine (*.mch)", "*.mch");
    fileChooser.getExtensionFilters().add(extFilter);
    final File file = fileChooser.showOpenDialog(stageProperty.get());
    if (file == null) {
      return;
    }
    final Thread loadMachineThread = new Thread(() -> {
      uiService.resetCurrentVarBindings();
      synthesisContextService.reset();
      proBApiService.reset();
      final SpecificationType specificationType = proBApiService.loadMachine(file);
      if (specificationType != null) {
        synthesisContextService.setSpecificationType(specificationType);
        synthesisContextService.contextEventStream().push(ContextEvent.RESET_CONTEXT);
      }
    });
    loadMachineThread.setDaemon(true);
    loadMachineThread.start();
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
    openSynthesisTab();
    synthesisInfoBox.reset();
    synthesisContextService.reset();
    validationPane.getNodes().clear();
    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    synthesisContextService.setCurrentOperation(operationNameOptional.get());
    uiService.resetCurrentVarBindings();
    synthesisInfoBox.showInfoProperty().set(true);
  }

  /**
   * Change the synthesis type to invariants.
   */
  @FXML
  @SuppressWarnings("unused")
  public void modifyInvariants() {
    openSynthesisTab();
    uiService.resetCurrentVarBindings();
    synthesisContextService.reset();
    synthesisContextService.contextEventStream().push(ContextEvent.RESET_CONTEXT);
    synthesisContextService.modifyInvariantsProperty().set(true);
    Platform.runLater(() -> synthesisContextService.setSynthesisType(SynthesisType.INVARIANT));
    uiService.visualizeBehaviorEventSource().push(new MachineVisualization());
  }

  /**
   * Clear the {@link ValidationPane} and show the nodes from the trace found by the model checker.
   * Only enabled if the model checker found an error {@link
   * ModelCheckingService#errorTraceProperty()}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void showNodesFromTrace() {
    openSynthesisTab();
    proBApiService.reset();
    validationPane.getNodes().clear();
    final UncoveredError uncoveredError =
        modelCheckingService.resultProperty().get().getUncoveredError();
    if (uncoveredError.isInvariantViolation() || (uncoveredError.isDeadlock()
        && modelCheckingService.deadlockRepairProperty().get().isRemoveDeadlock())) {
      // invariant violation or deadlock where the user decided to remove the deadlock state from
      // the model, i.e., the precondition of the affected operation needs to be strengthened
      synthesisContextService.setSynthesisType(SynthesisType.GUARD);
      validationPane.initializeNodesFromTrace();
      return;
    }
    // the user decided to resolve the deadlock state s by synthesizing a new operation to
    // transition from s to another state s'
    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    validationPane.initializeDeadlockResolveFromTrace();
  }

  /**
   * Visualize an existing operation by collecting several transitions.
   */
  @FXML
  @SuppressWarnings("unused")
  public void visualizeOperation() {
    openSynthesisTab();
    synthesisContextService.reset();
    uiService.resetCurrentVarBindings();
    validationPane.getNodes().clear();
    final Optional<String> operationName = getExistingOperationName();
    operationName.ifPresent(s -> uiService.visualizeBehaviorEventSource().push(
        new MachineVisualization(VisualizationType.OPERATION, s)));
  }

  private void openSynthesisTab() {
    uiService.applicationEventStream().push(
        new ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.SYNTHESIS));
  }

  /**
   * Run the model checker and display the progress using the {@link
   * ModelCheckingProgressIndicator}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void runModelChecking() {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    if (stateSpace == null) {
      return;
    }
    openSynthesisTab();
    uiService.resetCurrentVarBindings();
    validationPane.reset();
    synthesisContextService.reset();
    modelCheckingService.stateSpaceEventStream().push(stateSpace);
  }

  /**
   * Show a dialog to set the solver timeout using {@link SetPreferenceCommand}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void setTimeout() {
    final Optional<String> timeoutOptional = getTimeoutFromDialog();
    timeoutOptional.ifPresent(timeout -> {
      final SetPreferenceCommand setPreferenceCommand =
          new SetPreferenceCommand("TIME_OUT", timeout);
      synthesisContextService.getStateSpace().execute(setPreferenceCommand);
    });
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void stopModelChecking() {
    openSynthesisTab();
    modelCheckingService.runningProperty().set(false);
    modelCheckingService.indicatorPresentProperty().set(false);
  }

  /**
   * Save the machine as.
   */
  @FXML
  @SuppressWarnings("unused")
  public void saveAs() {
    openSynthesisTab();
    synthesisContextService.contextEventStream().push(ContextEvent.SAVE_AS);
  }

  /**
   * Validate all state nodes by checking the current machine invariants on each state.
   */
  @FXML
  @SuppressWarnings("unused")
  public void verifyAllNodes() {
    validationPane.getNodes().forEach(basicNode ->
        new Thread(() -> {
          if (basicNode instanceof TransitionNode) {
            final TransitionNode transitionNode = (TransitionNode) basicNode;
            transitionNode.validateTransition();
            return;
          }
          ((StateNode) basicNode).validateState();
        }).start());
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void zoomIn() {
    openSynthesisTab();
    uiService.zoomEventStream().push(UiService.UiZoom.ZOOM_IN);
  }

  /**
   * Interrupt the model checker.
   */
  @FXML
  @SuppressWarnings("unused")
  public void zoomOut() {
    openSynthesisTab();
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
    if (!synthesisContextService.useDefaultLibrary()
        && synthesisContextService.getSelectedLibraryComponents().isEmpty()) {
      // cancel if default configuration not selected but also no selected library components
      // are given
      uiService.applicationEventStream().push(
          new ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.LIBRARY_CONFIGURATION));
      return;
    }
    openSynthesisTab();
    final List<BasicNode> invalidNodes = validationPane.getInvalidNodes();
    addPredecessorNodesIfGuard(invalidNodes);
    final List<BasicNode> validNodes = validationPane.getValidNodes();
    final HashMap<String, List<BasicNode>> examples = new HashMap<>();
    examples.put("valid", validNodes);
    examples.put("invalid", invalidNodes);
    // reset the library expansion to 1 but if a synthesis instance has been suspended the
    // {@link ProBApiService} will restart this instance with the last expansion used on this
    // statespace
    synthesisContextService.selectedLibraryComponentsProperty().get().setLibraryExpansion(1);
    final StartSynthesisCommand startSynthesisCommand = new StartSynthesisCommand(
        synthesisContextService.selectedLibraryComponentsProperty().get(),
        synthesisContextService.getCurrentOperation(),
        uiService.getCurrentVarNames(),
        synthesisContextService.synthesisTypeProperty().get(),
        examples, synthesisContextService.solverBackendProperty().get());
    proBApiService.startSynthesisEventSource().push(startSynthesisCommand);
  }

  /**
   * Stop synthesis.
   */
  @FXML
  @SuppressWarnings("unused")
  public void stopSynthesis() {
    proBApiService.reset();
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
   * Clear the {@link ValidationPane} by deleting all available nodes.
   */
  @FXML
  @SuppressWarnings("unused")
  public void clear() {
    openSynthesisTab();
    validationPane.getNodes().clear();
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
    uiService.applicationEventStream().push(
        new ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.LIBRARY_CONFIGURATION));
  }

  /**
   * Expand all nodes on the {@link ValidationPane}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void expandAllNodes() {
    openSynthesisTab();
    validationPane.expandAllNodes();
  }

  /**
   * Shrink all nodes on the {@link ValidationPane}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void shrinkAllNodes() {
    openSynthesisTab();
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
    textInputDialog.getEditor().clear();
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
        getTextInputDialog("Set new timeout", "Solver timeout in milliseconds:");
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
    // display the current timeout value as the prompt text
    final GetPreferenceCommand getPreferenceCommand = new GetPreferenceCommand("TIME_OUT");
    synthesisContextService.getStateSpace().execute(getPreferenceCommand);
    textInputDialog.getEditor().setText(getPreferenceCommand.getValue());
    return textInputDialog;
  }

  /**
   * Show a {@link ChoiceDialog} to choose an operation to be visualized.
   */
  private Optional<String> getExistingOperationName() {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final GetMachineOperationNamesCommand getMachineOperationNamesCommand =
        new GetMachineOperationNamesCommand();
    stateSpace.execute(getMachineOperationNamesCommand);
    final List<String> operationNames = getMachineOperationNamesCommand.getMachineOperationNames();
    if (operationNames.isEmpty()) {
      final Alert alert = new Alert(Alert.AlertType.ERROR,
          "The machine currently has no operations.", ButtonType.OK);
      alert.showAndWait();
      return Optional.empty();
    }
    final ChoiceDialog<String> choiceDialog =
        new ChoiceDialog<>(operationNames.get(0), operationNames);
    choiceDialog.setTitle("Visualize Operation");
    choiceDialog.setHeaderText(null);
    choiceDialog.setContentText("Choose an operation to be visualized");
    return choiceDialog.showAndWait();
  }

  /**
   * An operation name is valid if it doesn't already exist and does not contain any special
   * character except of underscore '_'.
   */
  private boolean isValidOperationName(final String operationName) {
    if (operationName.isEmpty()) {
      return false;
    }
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final GetMachineOperationNamesCommand getMachineOperationNamesCommand =
        new GetMachineOperationNamesCommand();
    stateSpace.execute(getMachineOperationNamesCommand);
    if (getMachineOperationNamesCommand.getMachineOperationNames().contains(operationName)) {
      return false;
    }
    final Pattern p = Pattern.compile("[^a-z0-9_ ]", Pattern.CASE_INSENSITIVE);
    return operationName.split(" ").length == 1
        && !p.matcher(operationName).find();
  }
}
