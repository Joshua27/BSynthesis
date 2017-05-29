package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.components.SynthesisMainMenu;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.Variable;
import de.prob.statespace.StateSpace;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The main ui controller presenting a {@link #tabPane} and a {@link #synthesisMainMenu}.
 */
public class SynthesisMain extends VBox implements Initializable {

  private final SynthesisContextService synthesisContextService;

  @FXML
  @SuppressWarnings("unused")
  private TabPane tabPane;
  @FXML
  @SuppressWarnings("unused")
  private ValidationPane validationPane;
  @FXML
  @SuppressWarnings("unused")
  private CodeView codeView;
  @FXML
  @SuppressWarnings("unused")
  private LibraryConfiguration libraryConfiguration;
  @FXML
  @SuppressWarnings("unused")
  private Tab synthesisTab;
  @FXML
  @SuppressWarnings("unused")
  private Tab libraryConfigurationTab;
  @FXML
  @SuppressWarnings("unused")
  private SynthesisMainMenu synthesisMainMenu;

  /**
   * Set the {@link SynthesisContextService} and load the fxml resources.
   */
  @Inject
  public SynthesisMain(final FXMLLoader loader,
                       final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;

    Loader.loadFxml(loader, this, "synthesis_main.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tabPane.getTabs().remove(libraryConfigurationTab);
    setInitialVariableNames();

    synthesisContextService.stateSpaceProperty().addListener((observable, oldValue, newValue) ->
        setInitialVariableNames());

    synthesisContextService.showLibraryConfigurationProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue) {
            tabPane.getTabs().add(libraryConfigurationTab);
            tabPane.getSelectionModel().select(libraryConfigurationTab);
          }
        });
    libraryConfigurationTab.setOnClosed(event -> {
      synthesisContextService.showLibraryConfigurationProperty().set(false);
      tabPane.getSelectionModel().selectFirst();
    });
    synthesisContextService.showSynthesisTabProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue) {
            tabPane.getSelectionModel().select(synthesisTab);
          }
        });
    tabPane.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (!synthesisTab.equals(newValue)) {
            synthesisContextService.showSynthesisTabProperty().set(false);
          } else if (!libraryConfigurationTab.equals(newValue)) {
            synthesisContextService.showLibraryConfigurationProperty().set(false);
          }
        }
    );
  }

  private void setInitialVariableNames() {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    if (stateSpace == null) {
      return;
    }
    final ObservableSet<String> variableNames = FXCollections.observableSet();
    final AbstractElement mainComponent = stateSpace.getMainComponent();
    mainComponent.getChildrenOfType(Variable.class)
        .forEach(variableName -> variableNames.add(variableName.getName()));
    synthesisContextService.setMachineVarNames(variableNames);
  }

  public void setStage(final Stage stage) {
    synthesisMainMenu.stageProperty().set(stage);
  }
}
