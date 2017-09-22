package de.hhu.stups.bsynthesis.services;

public class ValidationPaneEvent {

  private final ValidationPaneEventType validationPaneEventType;

  public ValidationPaneEvent(final ValidationPaneEventType validationPaneEventType) {
    this.validationPaneEventType = validationPaneEventType;
  }

  public ValidationPaneEventType getValidationPaneEventType() {
    return validationPaneEventType;
  }
}
