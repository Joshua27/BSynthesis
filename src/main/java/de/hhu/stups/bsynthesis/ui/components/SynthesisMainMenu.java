package de.hhu.stups.bsynthesis.ui.components

import com.google.inject.Inject

import de.hhu.stups.bsynthesis.prob.GetMachineOperationNamesCommand
import de.hhu.stups.bsynthesis.prob.StartSynthesisCommand
import de.hhu.stups.bsynthesis.prob.SynthesizeImplicitIfStatements
import de.hhu.stups.bsynthesis.services.ApplicationEvent
import de.hhu.stups.bsynthesis.services.ApplicationEventType
import de.hhu.stups.bsynthesis.services.ControllerTab
import de.hhu.stups.bsynthesis.services.DaemonThread
import de.hhu.stups.bsynthesis.services.MachineVisualization
import de.hhu.stups.bsynthesis.services.ModelCheckingService
import de.hhu.stups.bsynthesis.services.ProBApiService
import de.hhu.stups.bsynthesis.services.ServiceDelegator
import de.hhu.stups.bsynthesis.services.SolverBackend
import de.hhu.stups.bsynthesis.services.SpecificationType
import de.hhu.stups.bsynthesis.services.SynthesisContextService
import de.hhu.stups.bsynthesis.services.UiService
import de.hhu.stups.bsynthesis.services.UiZoom
import de.hhu.stups.bsynthesis.services.UncoveredError
import de.hhu.stups.bsynthesis.services.ValidationPaneEvent
import de.hhu.stups.bsynthesis.services.ValidationPaneEventType
import de.hhu.stups.bsynthesis.services.VisualizationType
import de.hhu.stups.bsynthesis.ui.ContextEvent
import de.hhu.stups.bsynthesis.ui.ContextEventType
import de.hhu.stups.bsynthesis.ui.Loader
import de.hhu.stups.bsynthesis.ui.SynthesisType
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary
import de.hhu.stups.bsynthesis.ui.components.library.ConsiderIfType
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane
import de.prob.animator.command.SetPreferenceCommand
import de.prob.statespace.StateSpace

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableSet
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ChoiceDialog
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.TextInputDialog
import javafx.stage.FileChooser
import javafx.stage.Stage

import org.apache.commons.lang.math.NumberUtils
import org.fxmisc.easybind.EasyBind

import java.io.File
import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.Optional
import java.util.ResourceBundle
import java.util.regex.Pattern

class SynthesisMainMenu
/**
 * Initialize the variables derived by the injector and load the fxml resource.
 */
@Inject
constructor(loader: FXMLLoader,
            private val validationPane: ValidationPane,
            private val synthesisInfoBox: SynthesisInfoBox,
            serviceDelegator: ServiceDelegator,
            private val synthesizeImplicitIfStatements: SynthesizeImplicitIfStatements) : MenuBar(), Initializable {

    private val stageProperty: ObjectProperty<Stage>
    private val synthesisContextService: SynthesisContextService
    private val modelCheckingService: ModelCheckingService
    private val synthesisRunningProperty: BooleanProperty
    private val ignoreModelCheckerProperty: BooleanProperty
    private val uiService: UiService
    private val proBApiService: ProBApiService

    @FXML
    private val menuItemNew: MenuItem? = null
    @FXML
    private val menuItemOpen: MenuItem? = null
    @FXML
    private val menuItemSave: MenuItem? = null
    @FXML
    private val menuItemSaveAs: MenuItem? = null
    @FXML
    private val menuItemClear: MenuItem? = null
    @FXML
    private val menuItemNewOperation: MenuItem? = null
    @FXML
    private val menuItemModifyInvariants: MenuItem? = null
    @FXML
    private val menuItemVisualizeOperation: MenuItem? = null
    @FXML
    private val menuItemCheckModel: MenuItem? = null
    @FXML
    private val menuItemStopCheckModel: MenuItem? = null
    @FXML
    private val menuItemRunSynthesis: MenuItem? = null
    @FXML
    private val menuItemStopSynthesis: MenuItem? = null
    @FXML
    private val menuItemSetTimeout: MenuItem? = null
    @FXML
    private val menuItemConfigureLibrary: MenuItem? = null
    @FXML
    private val checkMenuItemInfo: CheckMenuItem? = null
    @FXML
    private val checkMenuItemIgnoreChecker: CheckMenuItem? = null
    @FXML
    private val menuItemExpandAll: MenuItem? = null
    @FXML
    private val menuItemShrinkAll: MenuItem? = null
    @FXML
    private val menuItemVerifyAllNodes: MenuItem? = null
    @FXML
    private val menuItemNodesFromTrace: MenuItem? = null
    @FXML
    private val menuItemZoomIn: MenuItem? = null
    @FXML
    private val menuItemZoomOut: MenuItem? = null
    @FXML
    private val radioMenuItemProB: MenuItem? = null
    @FXML
    private val radioMenuItemZ3: MenuItem? = null

    /**
     * Show a [TextInputDialog] and ask the user for a name for the new operation.
     */
    private val operationNameFromDialog: Optional<String>
        get() {
            val textInputDialog = getTextInputDialog("New Operation", "Set a name for the new operation:", "")
            textInputDialog.editor.clear()
            val operationNameOptional = textInputDialog.showAndWait()
            return if (operationNameOptional.isPresent && !isValidOperationName(operationNameOptional.get())) {
                operationNameFromDialog
            } else operationNameOptional
        }

    /**
     * Show a [TextInputDialog] to set the ProB solver timeout.
     */
    private val timeoutFromDialog: Optional<String>
        get() {
            val textInputDialog = getTextInputDialog("Set new timeout", "Solver timeout in milliseconds:",
                    synthesisContextService.solverTimeOut!!.toString())
            val timeoutOptional = textInputDialog.showAndWait()
            return if (timeoutOptional.isPresent && (!NumberUtils.isNumber(timeoutOptional.get()) || java.lang.Double.valueOf(timeoutOptional.get()).toInt() <= 0)) {
                timeoutFromDialog
            } else timeoutOptional
        }

    /**
     * Show a [ChoiceDialog] to choose an operation to be visualized.
     */
    private val existingOperationName: Optional<String>
        get() {
            val stateSpace = synthesisContextService.stateSpace
            val getMachineOperationNamesCommand = GetMachineOperationNamesCommand()
            stateSpace.execute(getMachineOperationNamesCommand)
            val operationNames = getMachineOperationNamesCommand.machineOperationNames
            if (operationNames.isEmpty()) {
                val alert = Alert(Alert.AlertType.ERROR,
                        "The machine currently has no operations.", ButtonType.OK)
                alert.showAndWait()
                return Optional.empty()
            }
            val choiceDialog = ChoiceDialog(operationNames[0], operationNames)
            choiceDialog.title = "Visualize Operation"
            choiceDialog.headerText = null
            choiceDialog.contentText = "Choose an operation to be visualized"
            return choiceDialog.showAndWait()
        }

    init {
        this.synthesisContextService = serviceDelegator.synthesisContextService()
        this.modelCheckingService = serviceDelegator.modelCheckingService()
        this.uiService = serviceDelegator.uiService()
        this.proBApiService = serviceDelegator.proBApiService()
        stageProperty = SimpleObjectProperty()
        synthesisRunningProperty = SimpleBooleanProperty(false)
        ignoreModelCheckerProperty = SimpleBooleanProperty(false)

        Loader.loadFxml(loader, this, "synthesis_main_menu.fxml")
    }

    override fun initialize(location: URL, resources: ResourceBundle) {
        initializeMenuItemBindings()
        radioMenuItemProB!!.onActionProperty().addListener { observable, oldValue, newValue -> synthesisContextService.solverBackendProperty().set(SolverBackend.PROB) }
        radioMenuItemZ3!!.onActionProperty().addListener { observable, oldValue, newValue -> synthesisContextService.solverBackendProperty().set(SolverBackend.Z3) }
        // synchronize statespaces after model checking
        EasyBind.subscribe(modelCheckingService.resultProperty()) { modelCheckingResult -> proBApiService.synchronizeStateSpaces() }

        synthesisContextService.contextEventStream().subscribe { contextEvent ->
            if (contextEvent != null && contextEvent.contextEventType == ContextEventType.LOAD) {
                loadMachine(contextEvent.file)
            }
        }

        synthesisRunningProperty.bind(proBApiService.synthesisRunningProperty())
    }

    private fun initializeMenuItemBindings() {
        val disableMenu = synthesisContextService.stateSpaceProperty().isNull
                .or(synthesisRunningProperty).or(modelCheckingService.indicatorPresentProperty())
        val extendMachineDisabled = disableMenu
                .or(Bindings.`when`(ignoreModelCheckerProperty).then(false)
                        .otherwise(modelCheckingService.errorTraceProperty().isNotNull
                                .or(modelCheckingService.resultProperty().isNull)
                                .or(modelCheckingService.invariantViolationInitialState())))
        menuItemNew!!.disableProperty().bind(synthesisContextService.synthesisRunningProperty())
        menuItemOpen!!.disableProperty().bind(synthesisContextService.synthesisSucceededProperty())
        menuItemClear!!.disableProperty().bind(disableMenu)
        menuItemSave!!.disableProperty().bind(disableMenu.or(uiService.codeHasChangedProperty().not()))
        menuItemSaveAs!!.disableProperty().bind(disableMenu)
        menuItemExpandAll!!.disableProperty().bind(disableMenu)
        menuItemShrinkAll!!.disableProperty().bind(disableMenu)
        menuItemCheckModel!!.disableProperty().bind(disableMenu
                .or(modelCheckingService.runningProperty())
                .or(ignoreModelCheckerProperty)
                .or(modelCheckingService.resultProperty().isNotNull))
        menuItemStopCheckModel!!.disableProperty().bind(disableMenu
                .or(modelCheckingService.runningProperty().not()))
        menuItemVisualizeOperation!!.disableProperty().bind(extendMachineDisabled)
        menuItemNewOperation!!.disableProperty().bind(extendMachineDisabled)
        menuItemModifyInvariants!!.disableProperty().bind(extendMachineDisabled)
        menuItemNodesFromTrace!!.disableProperty().bind(disableMenu
                .or(ignoreModelCheckerProperty)
                .or(modelCheckingService.invariantViolationInitialState())
                .or(modelCheckingService.errorTraceProperty().isNull))
        checkMenuItemInfo!!.selectedProperty().bindBidirectional(synthesisInfoBox.showInfoProperty())
        checkMenuItemInfo.disableProperty().bind(disableMenu
                .or(synthesisContextService.synthesisTypeProperty().isEqualTo(SynthesisType.NONE)))
        checkMenuItemIgnoreChecker!!.selectedProperty().bindBidirectional(ignoreModelCheckerProperty)
        checkMenuItemIgnoreChecker.disableProperty().bind(disableMenu
                .or(modelCheckingService.runningProperty()))
        menuItemVerifyAllNodes!!.disableProperty().bind(disableMenu
                .or(synthesisRunningProperty)
                .or(validationPane.nodes.emptyProperty()))
        menuItemRunSynthesis!!.disableProperty().bind(disableMenu
                .or(validationPane.nodes.emptyProperty())
                .or(synthesisRunningProperty))
        menuItemStopSynthesis!!.disableProperty().bind(synthesisRunningProperty.not())
        menuItemConfigureLibrary!!.disableProperty()
                .bind(synthesisContextService.synthesisSucceededProperty())
        menuItemZoomIn!!.disableProperty().bind(uiService.zoomInEnabledProperty().not())
        menuItemZoomOut!!.disableProperty().bind(uiService.zoomOutEnabledProperty().not())
        menuItemSetTimeout!!.disableProperty().bind(disableMenu)
    }

    /**
     * Create a new default machine.
     */
    @FXML
    fun newMachine() {
        synthesisContextService.contextEventStream()
                .push(ContextEvent(ContextEventType.NEW, null))
        uiService.applicationEventStream().push(
                ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.CODEVIEW))
    }

    /**
     * Load a machine from a .mch file.
     */
    @FXML
    fun loadMachine() {
        val fileChooser = FileChooser()
        val extFilter = FileChooser.ExtensionFilter("Machine (*.mch)", "*.mch")
        fileChooser.extensionFilters.add(extFilter)
        val file = fileChooser.showOpenDialog(stageProperty.get()) ?: return
        loadMachine(file)
    }

    private fun loadMachine(file: File) {
        DaemonThread.getDaemonThread {
            uiService.resetCurrentVarBindings()
            synthesisContextService.reset()
            proBApiService.reset()
            val specificationType = proBApiService.loadMachine(file)
            if (specificationType != null) {
                synthesisContextService.specificationType = specificationType
                synthesisContextService.contextEventStream()
                        .push(ContextEvent(ContextEventType.RESET_CONTEXT, null))
            }
        }.start()
    }

    /**
     * Create a new operation using synthesis. Clear the [ValidationPane] and set specific
     * context properties. The user is asked to specify a name for the operation to be synthesized.
     */
    @FXML
    fun newOperation() {
        val operationNameOptional = operationNameFromDialog
        if (!operationNameOptional.isPresent) {
            return
        }
        openSynthesisTab()
        synthesisInfoBox.reset()
        synthesisContextService.reset()
        validationPane.nodes.clear()
        synthesisContextService.synthesisType = SynthesisType.ACTION
        synthesisContextService.currentOperation = operationNameOptional.get()
        uiService.resetCurrentVarBindings()
        synthesisInfoBox.showInfoProperty().set(true)
    }

    /**
     * Change the synthesis type to invariants.
     */
    @FXML
    fun modifyInvariants() {
        openSynthesisTab()
        uiService.resetCurrentVarBindings()
        synthesisContextService.reset()
        synthesisContextService.modifyInvariantsProperty().set(true)
        uiService.validationPaneEventSource().push(
                ValidationPaneEvent(ValidationPaneEventType.CLEAR))
        Platform.runLater { synthesisContextService.synthesisType = SynthesisType.INVARIANT }
        uiService.visualizeBehaviorEventSource().push(MachineVisualization())
    }

    /**
     * Clear the [ValidationPane] and show the nodes from the trace found by the model checker.
     * Only enabled if the model checker found an error [ ][ModelCheckingService.errorTraceProperty].
     */
    @FXML
    fun showNodesFromTrace() {
        openSynthesisTab()
        proBApiService.reset()
        validationPane.nodes.clear()
        val uncoveredError = modelCheckingService.resultProperty().get().uncoveredError
        if (uncoveredError.isInvariantViolation || uncoveredError.isDeadlock && modelCheckingService.deadlockRepairProperty().get().isRemoveDeadlock) {
            // invariant violation or deadlock where the user decided to remove the deadlock state from
            // the model, i.e., the precondition of the affected operation needs to be strengthened
            synthesisContextService.synthesisType = SynthesisType.GUARD
            validationPane.initializeNodesFromTrace()
            return
        }
        // the user decided to resolve the deadlock state s by synthesizing a new operation to
        // transition from s to another state s'
        synthesisContextService.synthesisType = SynthesisType.ACTION
        validationPane.initializeDeadlockResolveFromTrace()
    }

    /**
     * Visualize an existing operation by collecting several transitions.
     */
    @FXML
    fun visualizeOperation() {
        openSynthesisTab()
        val operationName = existingOperationName
        operationName.ifPresent { s ->
            uiService.visualizeBehaviorEventSource().push(
                    MachineVisualization(VisualizationType.OPERATION, s))
            synthesisContextService.reset()
            uiService.resetCurrentVarBindings()
            validationPane.nodes.clear()
        }
    }

    private fun openSynthesisTab() {
        uiService.applicationEventStream().push(
                ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.SYNTHESIS))
    }

    /**
     * Run the model checker and display the progress using the [ ].
     */
    @FXML
    fun runModelChecking() {
        val stateSpace = synthesisContextService.stateSpace ?: return
        openSynthesisTab()
        uiService.resetCurrentVarBindings()
        validationPane.reset()
        synthesisContextService.reset()
        modelCheckingService.stateSpaceEventStream().push(stateSpace)
    }

    /**
     * Show a dialog to set the solver timeout using [SetPreferenceCommand].
     */
    @FXML
    fun setTimeout() {
        val timeoutOptional = timeoutFromDialog
        timeoutOptional.ifPresent { timeout -> synthesisContextService.solverTimeOut = Integer.valueOf(timeout) }
    }

    /**
     * Interrupt the model checker.
     */
    @FXML
    fun stopModelChecking() {
        openSynthesisTab()
        modelCheckingService.runningProperty().set(false)
        modelCheckingService.indicatorPresentProperty().set(false)
    }

    /**
     * Save the machine as.
     */
    @FXML
    fun saveAs() {
        if (uiService.codeHasChangedProperty().get()) {
            modelCheckingService.reset()
        }
        synthesisContextService.contextEventStream()
                .push(ContextEvent(ContextEventType.SAVE_AS, null))
    }

    /**
     * Save the machine.
     */
    @FXML
    fun save() {
        modelCheckingService.reset()
        synthesisContextService.contextEventStream()
                .push(ContextEvent(ContextEventType.SAVE, null))
    }

    /**
     * Validate all state nodes by checking the current machine invariants on each state.
     */
    @FXML
    fun verifyAllNodes() {
        validationPane.nodes.forEach { basicNode ->
            Thread {
                if (basicNode is TransitionNode) {
                    basicNode.validateTransition()
                    return@new Thread(() -> {
                        if (basicNode instanceof TransitionNode) {
                            final TransitionNode basicNode = TransitionNode basicNode;
                            basicNode.validateTransition();
                            return;
                        }
                        (StateNode basicNode).validateState();
                    }).start
                }
                (basicNode as StateNode).validateState()
            }.start()
        }
    }

    /**
     * Interrupt the model checker.
     */
    @FXML
    fun zoomIn() {
        openSynthesisTab()
        uiService.zoomEventStream().push(UiZoom.ZOOM_IN)
    }

    /**
     * Interrupt the model checker.
     */
    @FXML
    fun zoomOut() {
        openSynthesisTab()
        uiService.zoomEventStream().push(UiZoom.ZOOM_OUT)
    }

    /**
     * Start the synthesis prolog backend by executing [StartSynthesisCommand].
     */
    @FXML
    fun runSynthesis() {
        if (synthesisRunningProperty.get()) {
            return
        }
        if (!synthesisContextService.useDefaultLibrary() && synthesisContextService.selectedLibraryComponents.isEmpty) {
            // cancel if default configuration not selected but also no selected library components
            // are given
            uiService.applicationEventStream().push(
                    ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.LIBRARY_CONFIGURATION))
            return
        }
        if (!synthesisContextService.synthesisType.isAction) {
            synthesisContextService.selectedLibraryComponents
                    .considerIfStatementsProperty().set(ConsiderIfType.NONE)
        }
        synthesisContextService.userEvaluatedSolutionProperty().set(false)
        openSynthesisTab()
        val invalidNodes = validationPane.invalidNodes
        addPredecessorNodesIfGuard(invalidNodes)
        val validNodes = validationPane.validNodes
        val examples = HashMap<String, List<BasicNode>>()
        examples["valid"] = validNodes
        examples["invalid"] = invalidNodes
        // reset the library expansion to 1 but if a synthesis instance has been suspended the
        // {@link ProBApiService} will restart this instance with the last expansion used on this
        // statespace
        val selectedLibrary = synthesisContextService.selectedLibraryComponentsProperty().get()
        selectedLibrary.libraryExpansion = 1
        if (selectedLibrary.considerIfStatementsProperty().get().isImplicit) {
            synthesizeImplicitIfStatements.startSynthesis(examples)
            return
        }
        val startSynthesisCommand = StartSynthesisCommand(
                selectedLibrary,
                synthesisContextService.currentOperation,
                uiService.currentVarNames,
                HashSet<CompoundPrologTerm>(),
                synthesisContextService.synthesisType,
                examples, synthesisContextService.solverBackend)
        proBApiService.startSynthesisEventSource().push(startSynthesisCommand)
    }

    /**
     * Stop synthesis.
     */
    @FXML
    fun stopSynthesis() {
        proBApiService.reset()
    }

    /**
     * Add the predecessor states of the negative states to the negative examples when strengthening a
     * guard. We want to block the state leading to the violating state and thus need the
     * predecessor.
     */
    private fun addPredecessorNodesIfGuard(invalidNodes: MutableList<BasicNode>) {
        if (SynthesisType.GUARD == synthesisContextService.synthesisTypeProperty().get()) {
            val predecessorNodes = ArrayList<StateNode>()
            invalidNodes.forEach { basicNode ->
                val stateNode = basicNode as StateNode
                val existingPredecessorNodes = stateNode.predecessorProperty().get()
                if (existingPredecessorNodes.isEmpty()) {
                    val predecessorNode = stateNode.predecessorFromTrace
                    if (predecessorNode != null) {
                        predecessorNodes.add(predecessorNode)
                    }
                } else {
                    existingPredecessorNodes.forEach { stateNode1 -> predecessorNodes.add(stateNode1 as StateNode) }
                }
            }
            invalidNodes.addAll(predecessorNodes)
        }
    }

    /**
     * Clear the [ValidationPane] by deleting all available nodes.
     */
    @FXML
    fun clear() {
        openSynthesisTab()
        validationPane.nodes.clear()
    }

    @FXML
    fun close() {
        stageProperty().get().close()
    }

    /**
     * Open the library configuration tab.
     */
    @FXML
    fun configureLibrary() {
        uiService.applicationEventStream().push(
                ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.LIBRARY_CONFIGURATION))
    }

    /**
     * Expand all nodes on the [ValidationPane].
     */
    @FXML
    fun expandAllNodes() {
        openSynthesisTab()
        validationPane.expandAllNodes()
    }

    /**
     * Shrink all nodes on the [ValidationPane].
     */
    @FXML
    fun shrinkAllNodes() {
        openSynthesisTab()
        validationPane.shrinkAllNodes()
    }

    fun stageProperty(): ObjectProperty<Stage> {
        return stageProperty
    }

    private fun getTextInputDialog(title: String,
                                   contentText: String,
                                   currentValue: String): TextInputDialog {
        val textInputDialog = TextInputDialog()
        textInputDialog.title = title
        textInputDialog.headerText = null
        textInputDialog.contentText = contentText
        textInputDialog.editor.text = currentValue
        return textInputDialog
    }

    /**
     * An operation name is valid if it doesn't already exist and does not contain any special
     * character except of underscore '_'.
     */
    private fun isValidOperationName(operationName: String): Boolean {
        if (operationName.isEmpty()) {
            return false
        }
        val stateSpace = synthesisContextService.stateSpace
        val getMachineOperationNamesCommand = GetMachineOperationNamesCommand()
        stateSpace.execute(getMachineOperationNamesCommand)
        if (getMachineOperationNamesCommand.machineOperationNames.contains(operationName)) {
            return false
        }
        val p = Pattern.compile("[^a-z0-9_ ]", Pattern.CASE_INSENSITIVE)
        return operationName.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size == 1 && !p.matcher(operationName).find()
    }
}
