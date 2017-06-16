package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.Loader;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

  private final SynthesisContextService synthesisContextService;
  private final StringProperty cachedMachineCode;

  @FXML
  @SuppressWarnings("unused")
  private CodeArea codeArea;
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
                  final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;
    cachedMachineCode = new SimpleStringProperty();

    Loader.loadFxml(loader, this, "code_view.fxml");
  }

  /**
   * Initialize the {@link #codeArea} and set a listener to load the machine code as soon as {@link
   * SynthesisContextService#stateSpaceProperty()} has changed.
   */
  @FXML
  public final void initialize() {
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
    codeArea.richChanges()
        .filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
        .subscribe(change -> {
          if (!codeArea.getText().isEmpty()) {
            codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
          }
        });
    codeArea.prefWidthProperty().bind(widthProperty());
    codeArea.prefHeightProperty().bind(heightProperty());

    synthesisContextService.stateSpaceProperty().addListener((observable, oldValue, newValue) ->
        loadMachineCode());
    synthesisContextService.contextEventStream().subscribe(this::handleContextEvent);

    synthesisContextService.modifiedMachineCodeProperty().addListener(
        (observable, oldValue, newValue) -> updateMachineCode(oldValue, newValue));

    validateSolutionBox.visibleProperty().bind(
        synthesisContextService.synthesisSucceededProperty());
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

  private void updateMachineCode(final String oldValue, final String newValue) {
    if (!newValue.equals(oldValue) && !newValue.isEmpty()) {
      Platform.runLater(() -> {
        cachedMachineCode.set(codeArea.getText());
        codeArea.clear();
        codeArea.appendText(newValue.replaceAll("\'", ""));
      });
      // TODO: Highlight synthesized code
      synthesisContextService.modifiedMachineCodeProperty().set("");
    }
  }

  /**
   * The solution is already displayed in the {@link #codeArea} so we just have to save the machine
   * and undo the highlighting of the synthesized code.
   */
  @FXML
  @SuppressWarnings("unused")
  public void applySolution() {
    saveMachineCode();
    // TODO: Unhighlight synthesized code
    synthesisContextService.contextEventStream().push(ContextEvent.RESET_CONTEXT);
    // synthesisContextService.synthesisSucceededProperty().set(false);
  }

  /**
   * Display the {@link #cachedMachineCode previous machine code} describing the machine without the
   * synthesized code. The state of the machine has not changed so there is nothing else to do.
   */
  @FXML
  @SuppressWarnings("unused")
  public void discardSolution() {
    resetMachineCode();
    synthesisContextService.synthesisSucceededProperty().set(false);
  }

  private void resetMachineCode() {
    codeArea.clear();
    codeArea.appendText(cachedMachineCode.get());
  }

  private void saveMachineCode() {
    final String destination = synthesisContextService.getStateSpace().getModel()
        .getModelFile().getPath();
    try (FileWriter fileWriter = new FileWriter(destination)) {
      final BufferedWriter out = new BufferedWriter(fileWriter);
      out.write(codeArea.getText());
      out.close();
    } catch (final IOException ioException) {
      final Logger logger = LoggerFactory.getLogger(getClass());
      logger.error("IOException when saving the machine to " + destination, ioException);
    }
  }

  private void saveMachineCodeAs() {
    // TODO
  }

  private void loadMachineCode() {
    codeArea.clear();
    if (synthesisContextService.getStateSpace() == null) {
      return;
    }
    try (final Stream<String> stream =
             Files.lines(Paths.get(synthesisContextService.getStateSpace().getModel()
                 .getModelFile().getPath()))) {
      stream.forEach(line -> codeArea.appendText(line + "\n"));
    } catch (final IOException exception) {
      final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
      logger.error("Error loading machine code", exception);
    }
    cachedMachineCode.set(codeArea.getText());
  }
}
