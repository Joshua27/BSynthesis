package de.hhu.stups.bsynthesis.ui.controller;

import static de.hhu.stups.bsynthesis.prob.BMachineMisc.getMachineVars;

import com.google.inject.Inject;

import de.hhu.stups.bsynthesis.services.ApplicationEvent;
import de.hhu.stups.bsynthesis.services.ApplicationEventType;
import de.hhu.stups.bsynthesis.services.ControllerTab;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.components.SynthesisMainMenu;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The main ui controller presenting a {@link #tabPane} and a {@link #synthesisMainMenu}.
 */
public class SynthesisMain extends VBox implements Initializable {

  private final ServiceDelegator serviceDelegator;
  private final SynthesisContextService synthesisContextService;

  @FXML
  @SuppressWarnings("unused")
  private TabPane tabPane;
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
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    Loader.loadFxml(loader, this, "synthesis_main.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    initializeTabs();
    final UiService uiService = serviceDelegator.uiService();
    uiService.applicationEventStream().subscribe(this::selectTab);
    EasyBind.subscribe(synthesisContextService.synthesisSucceededProperty(), succeeded ->
        serviceDelegator.uiService().applicationEventStream().push(
            new ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.CODEVIEW)));
    EasyBind.subscribe(synthesisContextService.stateSpaceProperty(), stateSpace -> {
      if (stateSpace != null) {
        uiService.initializeCurrentVarBindings(getMachineVars(stateSpace));
      }
    });
  }

  private void initializeTabs() {
    tabPane.getTabs().remove(libraryConfigurationTab);
    libraryConfigurationTab.disableProperty()
        .bind(serviceDelegator.synthesisContextService().synthesisSucceededProperty());
    libraryConfigurationTab.setOnClosed(event -> tabPane.getSelectionModel().selectFirst());
    synthesisTab.disableProperty().bind(
        serviceDelegator.synthesisContextService().synthesisSucceededProperty());
  }

  private void selectTab(final ApplicationEvent applicationEvent) {
    if (!applicationEvent.getApplicationEventType().isOpenTab()) {
      return;
    }
    final ControllerTab controllerTab = applicationEvent.getControllerTab();
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
