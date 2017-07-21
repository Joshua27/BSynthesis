package de.hhu.stups.bsynthesis.services;

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
