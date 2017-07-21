package de.hhu.stups.bsynthesis.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Providers;

import de.codecentric.centerdevice.MenuToolkit;
import de.hhu.stups.bsynthesis.services.ModelCheckingService;
import de.hhu.stups.bsynthesis.services.ProBApiService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.ui.components.ModelCheckingProgressIndicator;
import de.hhu.stups.bsynthesis.ui.components.SynthesisInfoBox;
import de.hhu.stups.bsynthesis.ui.components.SynthesisMainMenu;
import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.ValidationContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeHeader;
import de.hhu.stups.bsynthesis.ui.controller.CodeView;
import de.hhu.stups.bsynthesis.ui.controller.LibraryConfiguration;
import de.hhu.stups.bsynthesis.ui.controller.SynthesisMain;
import de.hhu.stups.bsynthesis.ui.controller.SynthesisView;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.MainModule;

import javafx.fxml.FXMLLoader;

import java.util.Locale;
import java.util.ResourceBundle;

public class BSynthesisModule extends AbstractModule {

  private static final boolean IS_MAC = System.getProperty("os.name", "")
      .toLowerCase().contains("mac");

  private final Locale locale = new Locale("en");
  private final ResourceBundle bundle = ResourceBundle.getBundle("bundles.bsynthesis", locale);

  @Override
  protected void configure() {
    install(new MainModule());

    bind(SynthesisMain.class);
    bind(SynthesisView.class);
    bind(SynthesisInfoBox.class);
    bind(SynthesisContextService.class);
    bind(UiService.class);
    bind(ProBApiService.class);
    bind(ModelCheckingService.class);
    bind(ServiceDelegator.class);
    bind(ModelCheckingProgressIndicator.class);
    bind(SynthesisMainMenu.class);
    bind(NodeHeader.class);
    bind(ValidationPane.class);
    bind(CodeView.class);
    bind(LibraryConfiguration.class);
    bind(Locale.class).toInstance(locale);
    bind(ResourceBundle.class).toInstance(bundle);
    if (IS_MAC) {
      bind(MenuToolkit.class).toInstance(MenuToolkit.toolkit(locale));
    } else {
      bind(MenuToolkit.class).toProvider(Providers.of(null));
    }

    install(new FactoryModuleBuilder().build(StateNodeFactory.class));
    install(new FactoryModuleBuilder().build(TransitionNodeFactory.class));
    install(new FactoryModuleBuilder().build(ValidationContextMenuFactory.class));
    install(new FactoryModuleBuilder().build(NodeContextMenuFactory.class));
  }

  /**
   * Provide the {@link FXMLLoader}.
   */
  @Provides
  public FXMLLoader provideLoader(final Injector injector,
                                  final GuiceBuilderFactory builderFactory,
                                  final ResourceBundle bundle) {
    final FXMLLoader fxmlLoader = new FXMLLoader();
    fxmlLoader.setBuilderFactory(builderFactory);
    fxmlLoader.setControllerFactory(injector::getInstance);
    fxmlLoader.setResources(bundle);
    return fxmlLoader;
  }
}
