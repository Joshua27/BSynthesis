package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.components.SynthesisMainMenu;

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

  private final ServiceDelegator serviceDelegator;

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
  private Tab codeViewTab;
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
                       final ServiceDelegator serviceDelegator) {
    this.serviceDelegator = serviceDelegator;
    Loader.loadFxml(loader, this, "synthesis_main.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    initializeTabs();
    tabPane.getTabs().remove(libraryConfigurationTab);
    serviceDelegator.uiService().showTabEventStream().subscribe(this::selectTab);

    serviceDelegator.synthesisContextService().synthesisSucceededProperty().addListener(
        (observable, oldValue, newValue) ->
            serviceDelegator.uiService().showTabEventStream().push(ControllerTab.CODEVIEW));
  }

  private void initializeTabs() {
    libraryConfigurationTab.disableProperty()
        .bind(serviceDelegator.synthesisContextService().synthesisSucceededProperty());
    libraryConfigurationTab.setOnClosed(event -> tabPane.getSelectionModel().selectFirst());
    synthesisTab.disableProperty().bind(
        serviceDelegator.synthesisContextService().synthesisSucceededProperty());
  }

  private void selectTab(final ControllerTab controllerTab) {
    switch (controllerTab) {
      case SYNTHESIS:
        tabPane.getSelectionModel().select(synthesisTab);
        break;
      case CODEVIEW:
        tabPane.getSelectionModel().select(codeViewTab);
        break;
      case LIBRARY_CONFIGURATION:
        if (!tabPane.getTabs().contains(libraryConfigurationTab)) {
          tabPane.getTabs().add(libraryConfigurationTab);
        }
        tabPane.getSelectionModel().select(libraryConfigurationTab);
        break;
      default:
        break;
    }
  }

  public void setStage(final Stage stage) {
    synthesisMainMenu.stageProperty().set(stage);
  }
}
