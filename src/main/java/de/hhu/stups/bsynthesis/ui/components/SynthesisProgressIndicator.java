package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.hhu.stups.bsynthesis.services.ProBApiService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import org.fxmisc.easybind.EasyBind;

import java.net.URL;
import java.util.ResourceBundle;

@Singleton
public class SynthesisProgressIndicator extends GridPane implements Initializable {

  private final SynthesisContextService synthesisContextService;
  private final ProBApiService probApiService;
  private final BooleanProperty indicatorPresentProperty;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private FontAwesomeIconView iconCancelSynthesis;
  @FXML
  @SuppressWarnings("unused")
  private Label lbStatus;

  @Inject
  public SynthesisProgressIndicator(final FXMLLoader loader,
                                    final ServiceDelegator serviceDelegator) {
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    this.probApiService = serviceDelegator.proBApiService();
    indicatorPresentProperty = new SimpleBooleanProperty();
    Loader.loadFxml(loader, this, "synthesis_progress_indicator.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    iconCancelSynthesis.setOnMouseClicked(event -> {
      probApiService.reset();
      indicatorPresentProperty.set(false);
    });
    visibleProperty().bind(indicatorPresentProperty);
    EasyBind.subscribe(synthesisContextService.synthesisRunningProperty(), running -> {
      if (running) {
        progressIndicator.setVisible(true);
        indicatorPresentProperty.set(true);
        Platform.runLater(() -> lbStatus.setText("Synthesis running."));
      } else {
        Platform.runLater(() -> {
          if (synthesisContextService.synthesisSucceededProperty().get()) {
            indicatorPresentProperty.set(false);
          } else {
            progressIndicator.setVisible(false);
            Platform.runLater(() -> lbStatus.setText("Synthesis failed."));
          }
        });
      }
    });
  }

  public BooleanProperty indicatorPresentProperty() {
    return indicatorPresentProperty;
  }
}
