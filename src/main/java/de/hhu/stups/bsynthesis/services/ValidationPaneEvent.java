package de.hhu.stups.bsynthesis.services;

import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;

public class ValidationPaneEvent {

  private final ValidationPaneEventType validationPaneEventType;
  private final BasicNode node;

  public ValidationPaneEvent(final ValidationPaneEventType validationPaneEventType) {
    this.validationPaneEventType = validationPaneEventType;
    node = null;
  }

  public ValidationPaneEvent(final ValidationPaneEventType validationPaneEventType,
                             final BasicNode node) {
    this.validationPaneEventType = validationPaneEventType;
    this.node = node;
  }

  public ValidationPaneEventType getValidationPaneEventType() {
    return validationPaneEventType;
  }

  public BasicNode getNode() {
    return node;
  }
}
