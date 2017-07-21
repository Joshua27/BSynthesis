package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.ProBApiService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.Loader;

import de.prob.statespace.StateSpace;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.stage.FileChooser;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A simple text area to display the current machine code.
 */
public final class CodeView extends VBox {

  private static final String[] KEYWORDS = new String[] {
      "MACHINE", "ABSTRACT_VARIABLES", "VARIABLES", "INVARIANT", "INITIALISATION", "BEGIN", "END",
      "OPERATIONS", "PROPERTIES", "SETS", "LET", "ANY", "VAR", "SEES", "PRE", "THEN", "END;"
  };
  private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
  private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
  private static final Pattern PATTERN = Pattern.compile("(?<KEYWORD>" + KEYWORD_PATTERN + ")"
      + "|(?<COMMENT>" + COMMENT_PATTERN + ")", Pattern.CASE_INSENSITIVE);

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final SynthesisContextService synthesisContextService;
  private final StringProperty cachedMachineCode;
  private final ProBApiService proBApiService;

  @FXML
  @SuppressWarnings("unused")
  private CodeArea codeArea;
  @FXML
  @SuppressWarnings("unused")
  private CodeArea codeAreaSynthesized;
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

  /**
   * Set the {@link SynthesisContextService} and load the fxml resource.
   */
  @Inject
  public CodeView(final FXMLLoader loader,
                  final ServiceDelegator serviceDelegator) {
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    this.proBApiService = serviceDelegator.proBApiService();
    cachedMachineCode = new SimpleStringProperty();

    Loader.loadFxml(loader, this, "code_view.fxml");
  }

  /**
   * Initialize the {@link #codeArea} and set a listener to load the machine code as soon as {@link
   * SynthesisContextService#stateSpaceProperty()} has changed.
   */
  @FXML
  public final void initialize() {
    splitPaneCodeAreas.getItems().remove(codeAreaSynthesized);

    initializeCodeArea(codeArea);
    initializeCodeArea(codeAreaSynthesized);

    synthesisContextService.stateSpaceProperty().addListener((observable, oldValue, newValue) ->
        loadMachineCode());
    synthesisContextService.contextEventStream().subscribe(this::handleContextEvent);

    synthesisContextService.modifiedMachineCodeProperty().addListener(
        (observable, oldValue, newValue) -> showModifiedMachineCode(oldValue, newValue));

    validateSolutionBox.visibleProperty().bind(
        synthesisContextService.synthesisSucceededProperty());
  }

  private void initializeCodeArea(final CodeArea codeArea) {
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    codeArea.richChanges()
        .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
        .subscribe(change -> {
          if (!codeArea.getText().isEmpty()) {
            Platform.runLater(() ->
                codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
          }
        });
    codeArea.prefHeightProperty().bind(heightProperty());
  }

  private void handleContextEvent(final ContextEvent contextEvent) {
    switch (contextEvent) {
      case SAVE:
        saveMachineCode();
        break;
      case SAVE_AS:
        saveMachineCodeAs();
        break;
      default:
        break;
    }
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
  private void showModifiedMachineCode(final String oldValue, final String newValue) {
    if (!newValue.equals(oldValue) && !newValue.isEmpty()) {
      Platform.runLater(() -> {
        splitPaneCodeAreas.getItems().add(1, codeAreaSynthesized);
        codeAreaSynthesized.clear();
        codeAreaSynthesized.appendText(newValue.replaceAll("\'", ""));
      });
      synthesisContextService.modifiedMachineCodeProperty().set("");
    }
  }

  /**
   * Copy the solution from {@link #codeAreaSynthesized} to {@link #codeArea} and save the machine
   * code.
   */
  @FXML
  @SuppressWarnings("unused")
  public void applySolution() {
    splitPaneCodeAreas.getItems().remove(codeAreaSynthesized);
    cachedMachineCode.set(codeArea.getText());
    codeArea.clear();
    codeArea.appendText(codeAreaSynthesized.getText());
    saveMachineCode();
    synthesisContextService.contextEventStream().push(ContextEvent.RESET_CONTEXT);
  }

  /**
   * Display the {@link #cachedMachineCode previous machine code} describing the machine without the
   * synthesized code. The state of the machine has not changed so there is nothing else to do.
   */
  @FXML
  @SuppressWarnings("unused")
  public void discardSolution() {
    splitPaneCodeAreas.getItems().remove(codeAreaSynthesized);
    synthesisContextService.synthesisSucceededProperty().set(false);
  }

  // TODO: provide a history of changed machine codes with undo/redo
  private void resetMachineCode() {
    codeArea.clear();
    codeArea.appendText(cachedMachineCode.get());
  }

  /**
   * Save the machine and synchronize the statespaces provided by {@link ProBApiService}.
   */
  private void saveMachineCode() {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    proBApiService.synchronizeStateSpaces();
    final String destination = stateSpace.getModel()
        .getModelFile().getPath();
    try (final Writer fileWriterStream =
             new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8)) {
      fileWriterStream.write(codeArea.getText());
    } catch (final IOException ioException) {
      logger.error("IOException when saving the machine to " + destination, ioException);
    }
  }

  private void saveMachineCodeAs() {
    final Path source = synthesisContextService.getStateSpace().getModel().getModelFile().toPath();
    final FileChooser fileChooser = new FileChooser();
    final String extensionFilterString =
        synthesisContextService.getSpecificationType().isClassicalB() ? "Machine (*.mch)"
            : "Machine (*.eventb)";
    final FileChooser.ExtensionFilter extFilter =
        new FileChooser.ExtensionFilter(extensionFilterString);
    fileChooser.getExtensionFilters().add(extFilter);
    fileChooser.setInitialFileName(source.getFileName().toString());
    final File file = fileChooser.showSaveDialog(this.getScene().getWindow());
    if (file != null) {
      final Path destination = file.toPath();
      try {
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException exception) {
        logger.error("Error saving machine " + source.getFileName() + " to "
            + destination.toString(), exception);
      }
    }
  }

  private void loadMachineCode() {
    codeArea.clear();
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
    cachedMachineCode.set(codeArea.getText());
  }
}
