package de.hhu.stups.bsynthesis.services;

import de.hhu.stups.bsynthesis.ui.components.factories.NodeContextMenuFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.hhu.stups.bsynthesis.ui.controller.ControllerTab;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.reactfx.EventSource;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UiService {

  private final EventSource<ControllerTab> showTabEventStream;
  private final EventSource<UiZoom> zoomEventStream;
  private final NodeContextMenuFactory nodeContextMenuFactory;
  private final StateNodeFactory stateNodeFactory;
  private final TransitionNodeFactory transitionNodeFactory;
  private final BooleanProperty zoomInEnabledProperty;
  private final BooleanProperty zoomOutEnabledProperty;

  /**
   * Initialize node factories and event sources.
   */
  @Inject
  public UiService(final NodeContextMenuFactory nodeContextMenuFactory,
                   final StateNodeFactory stateNodeFactory,
                   final TransitionNodeFactory transitionNodeFactory) {
    this.nodeContextMenuFactory = nodeContextMenuFactory;
    this.stateNodeFactory = stateNodeFactory;
    this.transitionNodeFactory = transitionNodeFactory;
    showTabEventStream = new EventSource<>();
    zoomEventStream = new EventSource<>();
    zoomInEnabledProperty = new SimpleBooleanProperty();
    zoomOutEnabledProperty = new SimpleBooleanProperty();
  }

  public BooleanProperty zoomInEnabledProperty() {
    return zoomInEnabledProperty;
  }

  public BooleanProperty zoomOutEnabledProperty() {
    return zoomOutEnabledProperty;
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

  public EventSource<UiZoom> zoomEventStream() {
    return zoomEventStream;
  }

  public enum UiZoom {
    ZOOM_IN, ZOOM_OUT;

    public boolean isZoomIn() {
      return this.equals(ZOOM_IN);
    }

    public boolean isZoomOut() {
      return this.equals(ZOOM_OUT);
    }
  }
}
