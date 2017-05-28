package de.hhu.stups.bsynthesis.ui.components.factories;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.prob.statespace.State;
import de.prob.statespace.Trace;

import javafx.geometry.Point2D;

import javax.annotation.Nullable;

public interface StateNodeFactory {
  @Inject
  StateNode create(@Nullable State state,
                   @Nullable Trace trace,
                   @Assisted Point2D position,
                   @Assisted NodeState nodeState);
}
