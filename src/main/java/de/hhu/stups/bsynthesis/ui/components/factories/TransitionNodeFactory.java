package de.hhu.stups.bsynthesis.ui.components.factories;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.prob.statespace.State;

import javax.annotation.Nullable;

import javafx.geometry.Point2D;

public interface TransitionNodeFactory {
  @Inject
  TransitionNode create(@Assisted("inputState") @Nullable final State inputState,
                        @Assisted("outputState") @Nullable final State outputState,
                        @Assisted final Point2D position,
                        @Assisted final NodeState nodeState);
}
