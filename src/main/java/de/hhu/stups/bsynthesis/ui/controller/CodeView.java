package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.DaemonThread;
import de.hhu.stups.bsynthesis.services.ProBApiService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.ContextEventType;
import de.hhu.stups.bsynthesis.ui.Loader;

import de.prob.statespace.StateSpace;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.FileChooser;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A simple text area to display the current machine code.
 */
public final class CodeView extends VBox {

  private static final String[] KEYWORDS = new String[] {
      "MACHINE", "ABSTRACT_VARIABLES", "VARIABLES", "INVARIANT", "INITIALISATION", "BEGIN", "END",
      "OPERATIONS", "PROPERTIES", "SETS", "LET", "ANY", "VAR", "SEES", "PRE", "THEN", "END;",
      "CONSTANTS", "DEFINITIONS", "IF", "ELSE"};
  private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
  private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
  private static final Pattern PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")"
      + "|(?<COMMENT>" + COMMENT_PATTERN + ")", Pattern.CASE_INSENSITIVE);

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final SynthesisContextService synthesisContextService;
  private final ProBApiService proBApiService;
  private final UiService uiService;
  private final BooleanProperty userEvaluatedSolutionProperty;

  private final VirtualizedScrollPane scrollPaneCodeArea;
  private final CodeArea codeArea;
  private final VirtualizedScrollPane scrollPaneCodeAreaSynthesized;
  private final CodeArea codeAreaSynthesized;
  private final StringProperty machineNameProperty;

  @FXML
  @SuppressWarnings("unused")
  private SplitPane splitPaneCodeAreas;
  @FXML
  @SuppressWarnings("unused")
  private HBox validateSolutionBox;
  @FXML
  @SuppressWarnings("unused")
  private Button btApplySolution;
  @FXML
  @SuppressWarnings("unused")
  private Button btDiscardSolution;

  // TODO: undo/redo history?

  /**
   * Set the {@link SynthesisContextService} and load the fxml resource.
   */
  @Inject
  public CodeView(final FXMLLoader loader,
                  final ServiceDelegator serviceDelegator) {
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    this.proBApiService = serviceDelegator.proBApiService();
    this.uiService = serviceDelegator.uiService();

    // Unfortunately, VirtualizedScrollPane doesn't support instantiation from fxml..
    // see https://github.com/TomasMikula/Flowless/issues/25
    codeArea = new CodeArea();
    scrollPaneCodeArea = new VirtualizedScrollPane<>(codeArea);
    codeAreaSynthesized = new CodeArea();
    scrollPaneCodeAreaSynthesized = new VirtualizedScrollPane<>(codeAreaSynthesized);
    userEvaluatedSolutionProperty = new SimpleBooleanProperty();
    machineNameProperty = new SimpleStringProperty("");

    synthesisContextService.userEvaluatedSolutionProperty()
        .bindBidirectional(userEvaluatedSolutionProperty);

    Loader.loadFxml(loader, this, "code_view.fxml");
  }

  /**
   * Initialize the {@link #codeArea} and set a listener to load the machine code as soon as {@link
   * SynthesisContextService#stateSpaceProperty()} has changed.
   */
  @FXML
  public final void initialize() {
    splitPaneCodeAreas.getItems().add(scrollPaneCodeArea);

    initializeCodeArea(codeArea);
    initializeCodeArea(codeAreaSynthesized);
    codeAreaSynthesized.setEditable(false);

    codeArea.setOnKeyPressed(event -> {
      if (!event.getCode().equals(KeyCode.CONTROL) && !event.getCode().equals(KeyCode.UP)
          && !event.getCode().equals(KeyCode.LEFT) && !event.getCode().equals(KeyCode.RIGHT)
          && !event.getCode().equals(KeyCode.DOWN) && !event.getCode().equals(KeyCode.S)
          && !event.getCode().equals(KeyCode.ALT)) {
        uiService.codeHasChangedProperty().set(true);
      }
    });

    EasyBind.subscribe(synthesisContextService.stateSpaceProperty(),
        stateSpace -> Platform.runLater(this::loadMachineCode));
    synthesisContextService.contextEventStream().subscribe(this::handleContextEvent);

    EasyBind.subscribe(synthesisContextService.modifiedMachineCodeProperty(), newValue -> {
      if (newValue == null || newValue.equals("none")) {
        return;
      }
      DaemonThread.getDaemonThread(() -> showModifiedMachineCode(newValue)).start();
    });

    EasyBind.subscribe(machineNameProperty, s -> {
      if (!s.isEmpty()) {
        codeArea.setEditable(true);
      }
    });

    EasyBind.subscribe(synthesisContextService.behaviorSatisfiedProperty(), operationName -> {
      if (operationName == null || synthesisContextService.selectedLibraryComponentsProperty()
          .get().considerIfStatementsProperty().get().isImplicit()) {
        return;
      }
      Platform.runLater(() -> {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Behavior already satisfied");
        alert.setHeaderText("");
        alert.setContentText("The provided behavior is already satisfied by "
            + operationName + ".");
        alert.showAndWait();
        synthesisContextService.synthesisSucceededProperty().set(false);
        synthesisContextService.behaviorSatisfiedProperty().set(null);
        synthesisContextService.modifiedMachineCodeProperty().set(null);
      });
    });

    uiService.applicationEventStream().subscribe(applicationEvent -> {
      if (applicationEvent.getApplicationEventType().isCloseApp()) {
        executorService.shutdown();
        proBApiService.shutdownExecutor();
      }
    });
  }

  private void initializeCodeArea(final CodeArea codeArea) {
    codeArea.setEditable(true);
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    codeArea.richChanges()
        .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
        .subscribe(change -> {
          if (!codeArea.getText().isEmpty() && (!change.getInserted().getText().isEmpty()
              || !change.getRemoved().getText().isEmpty())) {
            Platform.runLater(() ->
                codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
          }
        });
    codeArea.prefHeightProperty().bind(heightProperty());
  }

  private void handleContextEvent(final ContextEvent contextEvent) {
    switch (contextEvent.getContextEventType()) {
      case SAVE:
        saveMachineCode();
        break;
      case SAVE_AS:
        saveMachineCodeAs();
        break;
      case NEW:
        newMachine();
        break;
      default:
        break;
    }
  }

  private void newMachine() {
    final FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Machine (*.mch)", "*.mch"));
    final File file = fileChooser.showSaveDialog(this.getScene().getWindow());
    if (file == null) {
      return;
    }
    final String fileName = file.getName().replace(".mch", "");
    codeArea.clear();
    fileChooser.setInitialFileName(fileName);
    machineNameProperty.set(fileName);
    setNewMachineCode(fileName);
    saveMachineCode(file.getPath());
    final ContextEvent contextEvent = new ContextEvent(ContextEventType.LOAD, file);
    synthesisContextService.contextEventStream().push(contextEvent);
  }

  private StyleSpans<Collection<String>> computeHighlighting(final String text) {
    final Matcher matcher = PATTERN.matcher(text);
    int lastKwEnd = 0;
    final StyleSpansBuilder<Collection<String>> spansBuilder
        = new StyleSpansBuilder<>();
    while (matcher.find()) {
      final String styleClass = getHighlightingStyleClass(matcher);
      spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
      spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
      lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
  }

  private String getHighlightingStyleClass(final Matcher matcher) {
    if (matcher.group("KEYWORD") != null) {
      return "keyword";
    }
    if (matcher.group("COMMENT") != null) {
      return "comment";
    }
    return "";
  }

  /**
   * Show the modified machine code containing the synthesized changes.
   */
  private void showModifiedMachineCode(final String newValue) {
    if (newValue == null || userEvaluatedSolutionProperty.get()) {
      return;
    }
    validateSolutionBox.setVisible(true);
    codeArea.setEditable(false);
    if (!newValue.isEmpty()) {
      executorService.execute(() -> Platform.runLater(() -> {
        splitPaneCodeAreas.getItems().add(1, scrollPaneCodeAreaSynthesized);
        codeAreaSynthesized.clear();
        codeAreaSynthesized.appendText(newValue);
      }));
    }
  }

  /**
   * Copy the solution from {@link #codeAreaSynthesized} to {@link #codeArea} and save the machine
   * code.
   */
  @FXML
  @SuppressWarnings("unused")
  public void applySolution() {
    codeArea.setEditable(true);
    userEvaluatedSolutionProperty.set(true);
    splitPaneCodeAreas.getItems().remove(scrollPaneCodeAreaSynthesized);
    codeArea.clear();
    Platform.runLater(() -> {
      codeArea.appendText(codeAreaSynthesized.getText());
      saveMachineCode();
    });
    synthesisContextService.contextEventStream()
        .push(new ContextEvent(ContextEventType.RESET_CONTEXT, null));
    synthesisContextService.synthesisSucceededProperty().set(false);
    proBApiService.reset();
    validateSolutionBox.setVisible(false);
  }

  /**
   * Just hide the {@link #scrollPaneCodeAreaSynthesized}. The state of the machine has not changed
   * so that there is nothing else to do.
   */
  @FXML
  @SuppressWarnings("unused")
  public void discardSolution() {
    codeArea.setEditable(true);
    userEvaluatedSolutionProperty.set(true);
    splitPaneCodeAreas.getItems().remove(scrollPaneCodeAreaSynthesized);
    synthesisContextService.synthesisSucceededProperty().set(false);
    proBApiService.reset();
    validateSolutionBox.setVisible(false);
  }

  /**
   * Save the machine and synchronize the statespaces provided by {@link ProBApiService}.
   */
  private void saveMachineCode() {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final File modelFile = stateSpace.getModel().getModelFile();
    final String destination = modelFile.getPath();
    saveMachineCode(destination);
    // reload machine
    proBApiService.loadMachine(modelFile);
  }

  private void saveMachineCode(final String destination) {
    try (final Writer fileWriterStream =
             new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8)) {
      fileWriterStream.write(codeArea.getText());
    } catch (final IOException ioException) {
      logger.error("IOException when saving the machine to " + destination, ioException);
    }
    uiService.codeHasChangedProperty().set(false);
  }

  private void saveMachineCodeAs() {
    final Path source = synthesisContextService.getStateSpace() != null
        ? synthesisContextService.getStateSpace().getModel().getModelFile().toPath()
        : Paths.get("");
    if (source == null) {
      return;
    }
    final FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Machine (*.mch)", "*.mch"));
    final File file = fileChooser.showSaveDialog(this.getScene().getWindow());
    final String fileName = source.getFileName().toString();
    fileChooser.setInitialFileName(fileName);
    machineNameProperty.set(fileName);
    if (file != null) {
      final Path destination = file.toPath();
      try {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException exception) {
        logger.error("Error saving machine " + fileName + " to "
            + destination.toString(), exception);
      }
    }
    uiService.codeHasChangedProperty().set(false);
  }

  private void loadMachineCode() {
    codeArea.clear();
    // TODO: maybe use full_b_machine/1 instead of reading from file?
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    if (stateSpace == null || stateSpace.getModel().getModelFile() == null) {
      return;
    }
    try (final Stream<String> stream =
             Files.lines(Paths.get(stateSpace.getModel().getModelFile().getPath()))) {
      stream.forEach(line -> Platform.runLater(() -> codeArea.appendText(line + "\n")));
    } catch (final IOException exception) {
      logger.error("Error loading machine code", exception);
    }
    uiService.codeHasChangedProperty().set(false);
  }

  /**
   * Set the default new machine.
   */
  private void setNewMachineCode(final String machineName) {
    if (!machineName.isEmpty()) {
      codeArea.appendText("MACHINE " + machineName + "\n"
          + "SETS\n"
          + " ID={aa,bb}\n"
          + "CONSTANTS iv\n"
          + "PROPERTIES\n"
          + " iv:ID\n"
          + "VARIABLES xx\n"
          + "INVARIANT\n"
          + " xx:ID\n"
          + "INITIALISATION xx:=iv\n"
          + "OPERATIONS\n"
          + "  Set(yy) = PRE yy:ID THEN xx:= yy END\n"
          + "END\n");
    }
  }
}
