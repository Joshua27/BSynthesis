package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

public final class CodeView extends VBox {

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final SynthesisContextService synthesisContextService;

  @FXML
  @SuppressWarnings("unused")
  private CodeArea codeArea;

  @Inject
  public CodeView(final FXMLLoader loader,
                  final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;

    loader.setLocation(getClass().getResource("code_view.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (final IOException exception) {
      logger.log(Level.SEVERE, "Loading fxml for the code view failed.", exception);
    }
  }

  @FXML
  public final void initialize() {
    codeArea.setEditable(false);
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

    codeArea.prefWidthProperty().bind(widthProperty());
    codeArea.prefHeightProperty().bind(heightProperty());

    synthesisContextService.stateSpaceProperty().addListener((observable, oldValue, newValue) ->
        loadMachineCode());
  }

  private void loadMachineCode() {
    if (synthesisContextService.getStateSpace() == null) {
      return;
    }
    try (final Stream<String> stream =
             Files.lines(Paths.get(synthesisContextService.getStateSpace().getModel()
                 .getModelFile().getPath()))) {
      stream.forEach(line -> codeArea.appendText(line + "\n"));
    } catch (final IOException exception) {
      logger.log(Level.SEVERE, "Error loading machine code", exception);
    }
  }
}
