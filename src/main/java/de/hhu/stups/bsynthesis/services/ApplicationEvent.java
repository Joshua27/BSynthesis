package de.hhu.stups.bsynthesis.services;

public class ApplicationEvent {

  private final ApplicationEventType applicationEventType;
  private final ControllerTab controllerTab;

  public ApplicationEvent(final ApplicationEventType applicationEventType) {
    this.applicationEventType = applicationEventType;
    controllerTab = null;
  }

  public ApplicationEvent(final ApplicationEventType applicationEventType,
                          final ControllerTab controllerTab) {
    this.applicationEventType = applicationEventType;
    this.controllerTab = controllerTab;
  }

  public ApplicationEventType getApplicationEventType() {
    return applicationEventType;
  }

  public ControllerTab getControllerTab() {
    return controllerTab;
  }
}