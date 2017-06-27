package de.hhu.stups.bsynthesis.services;

import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.hhu.stups.bsynthesis.ui.controller.ControllerTab;
import org.reactfx.EventSource;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UiService {

  private final EventSource<ControllerTab> showTabEventStream;
  private final NodeContextMenuFactory nodeContextMenuFactory;
  private final StateNodeFactory stateNodeFactory;
  private final TransitionNodeFactory transitionNodeFactory;

  @Inject
  public UiService(final NodeContextMenuFactory nodeContextMenuFactory,
                   final StateNodeFactory stateNodeFactory,
                   final TransitionNodeFactory transitionNodeFactory) {
    this.nodeContextMenuFactory = nodeContextMenuFactory;
    this.stateNodeFactory = stateNodeFactory;
    this.transitionNodeFactory = transitionNodeFactory;
    showTabEventStream = new EventSource<>();
  }

  public NodeContextMenuFactory getNodeContextMenuFactory() {
    return nodeContextMenuFactory;
  }

  public StateNodeFactory getStateNodeFactory() {
    return stateNodeFactory;
  }

  public TransitionNodeFactory getTransitionNodeFactory() {
    return transitionNodeFactory;
  }

  public EventSource<ControllerTab> showTabEventStream() {
    return showTabEventStream;
  }
}
