package de.hhu.stups.bsynthesis.services;

import de.hhu.stups.bsynthesis.ui.ContextEvent;
import de.hhu.stups.bsynthesis.ui.ContextEventType;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServiceDelegator {

  private final SynthesisContextService synthesisContextService;
  private final ModelCheckingService modelCheckingService;
  private final UiService uiService;
  private final ProBApiService proBApiService;

  /**
   * Delegate services.
   */
  @Inject
  public ServiceDelegator(final SynthesisContextService synthesisContextService,
                          final ModelCheckingService modelCheckingService,
                          final UiService uiService,
                          final ProBApiService proBApiService) {
    this.synthesisContextService = synthesisContextService;
    this.modelCheckingService = modelCheckingService;
    this.uiService = uiService;
    this.proBApiService = proBApiService;
    setBindings();
  }

  private void setBindings() {
    synthesisContextService.synthesisRunningProperty()
        .bindBidirectional(proBApiService.synthesisRunningProperty());
    synthesisContextService.synthesisSuspendedProperty()
        .bindBidirectional(proBApiService.synthesisSuspendedProperty());
    synthesisContextService.synthesisSucceededProperty()
        .bindBidirectional(proBApiService.synthesisSucceededProperty());
    synthesisContextService.modifiedMachineCodeProperty()
        .bindBidirectional(proBApiService.modifiedMachineCodeProperty());
    synthesisContextService.behaviorSatisfiedProperty()
        .bindBidirectional(proBApiService.behaviorSatisfiedProperty());
    synthesisContextService.userEvaluatedSolutionProperty()
        .bindBidirectional(proBApiService.userEvaluatedSolutionProperty());
    synthesisContextService.useSingleThreadProperty()
        .bindBidirectional(proBApiService.useSingleThreadProperty());
    synthesisContextService.contextEventStream().subscribe(contextEvent -> {
      if (ContextEventType.RESET_CONTEXT.equals(contextEvent.getContextEventType())) {
        modelCheckingService.reset();
      }
    });
    EasyBind.subscribe(proBApiService.mainStateSpaceProperty(), stateSpace -> {
      uiService.applicationEventStream().push(
          new ApplicationEvent(ApplicationEventType.OPEN_TAB, ControllerTab.CODEVIEW));
      // bind one statespace to the synthesis context, the other instances are synchronized within
      // {@link ProBApiService} according to this statespace
      synthesisContextService.setStateSpace(proBApiService.getMainStateSpace());
    });
  }

  public SynthesisContextService synthesisContextService() {
    return synthesisContextService;
  }

  public ModelCheckingService modelCheckingService() {
    return modelCheckingService;
  }

  public UiService uiService() {
    return uiService;
  }

  public ProBApiService proBApiService() {
    return proBApiService;
  }
}
