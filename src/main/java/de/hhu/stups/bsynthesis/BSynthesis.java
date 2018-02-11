package de.hhu.stups.bsynthesis;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.hhu.stups.bsynthesis.injector.BSynthesisModule;
import de.hhu.stups.bsynthesis.services.ApplicationEvent;
import de.hhu.stups.bsynthesis.services.ApplicationEventType;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.controller.SynthesisMain;
import de.prob.cli.ProBInstanceProvider;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BSynthesis extends Application {

  private Injector injector;
  private UiService uiService;

  public static void main(final String... args) {
    launch(args);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    System.setProperty("logback.configurationFile", "config/logging.xml");

    injector = Guice.createInjector(new BSynthesisModule());

    uiService = injector.getInstance(UiService.class);
    final SynthesisMain root = injector.getInstance(SynthesisMain.class);
    final Scene mainScene = new Scene(root, 1024, 768);
    root.getStylesheets().add("main.css");
    root.setStage(stage);

    stage.setTitle("BSynthesis");
    stage.setScene(mainScene);
    stage.setOnCloseRequest(e -> Platform.exit());

    stage.show();
  }

  /**
   * Shut down all injector instances and send {@link ApplicationEventType#CLOSE_APP} to the
   * {@link UiService#applicationEventStream} to shut down all executor services etc.
   */
  @Override
  public void stop() {
    injector.getInstance(ProBInstanceProvider.class).shutdownAll();
    uiService.applicationEventStream().push(
        new ApplicationEvent(ApplicationEventType.CLOSE_APP));
    Platform.exit();
    System.exit(0);
  }
}