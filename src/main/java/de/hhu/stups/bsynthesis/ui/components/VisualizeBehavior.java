package de.hhu.stups.bsynthesis.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.prob.VarValueTuple;
import de.hhu.stups.bsynthesis.prob.VisualizeInvariantsCommand;
import de.hhu.stups.bsynthesis.prob.VisualizeOperationCommand;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.factories.StateNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.factories.TransitionNodeFactory;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeState;
import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.TransitionNode;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.animator.command.FindStateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visualize an existing operation using {@link #visualizeOperation(String, TransitionNodeFactory)}
 * or visualize the machine invariants. Here, visualizing means that we collect a specific amount
 * of sample transitions/states describing the behavior of an operation or the machine invariants
 * which are then added to the {@link ValidationPane}.
 */
@Singleton
public class VisualizeBehavior {

  private final SynthesisContextService synthesisContextService;
  private final int maxPerRow =
      (int) Math.floor((ValidationPane.WIDTH / 2) / (StateNode.WIDTH * 3));

  private final ListProperty<String> ignoredIDsProperty;

  @Inject
  public VisualizeBehavior(final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;
    ignoredIDsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  /**
   * Visualize the machine invariants by providing valid and invalid states.
   */
  public Map<String, Set<StateNode>> visualizeInvariants(final StateNodeFactory stateNodeFactory) {
    int validRow = 0;
    int validCol = 0;

    ignoredIDsProperty.clear();

    final VisualizeInvariantsCommand visualizeInvariantsCommand =
        new VisualizeInvariantsCommand(5, 5);
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    stateSpace.execute(visualizeInvariantsCommand);

    final List<String> validStateEqualities =
        visualizeInvariantsCommand.validStateEqualitiesProperty().get();
    final List<String> invalidStateEqualities =
        visualizeInvariantsCommand.invalidStateEqualitiesProperty().get();

    final Map<String, Set<StateNode>> stateNodes = new HashMap<>();
    final Set<StateNode> validStates = new HashSet<>();
    final Set<StateNode> invalidStates = new HashSet<>();
    stateNodes.put("valid", validStates);
    stateNodes.put("invalid", invalidStates);

    for (final String validStateEquality : validStateEqualities) {
      final Point2D pos = getPositionForRowAndCol(validRow, validCol);
      validStates.add(stateNodeFactory.create(getStateForEqualityPred(validStateEquality), null,
          pos, NodeState.TENTATIVE));
      validCol++;
      if (validCol == maxPerRow - 1) {
        validCol = 0;
        validRow++;
      }
    }
    final Map<State, Point2D> invalidTuples =
        getStatePositionTuplesForNegativeEqualities(invalidStateEqualities);
    invalidTuples.forEach((state, pos) ->
        invalidStates.add(stateNodeFactory.create(state, null, pos, NodeState.TENTATIVE)));
    return stateNodes;
  }

  /**
   * Return a map of tuples of {@link State} and a {@link Point2D} describing the position on the
   * {@link ValidationPane}.
   */
  private Map<State, Point2D> getStatePositionTuplesForNegativeEqualities(
      final List<String> invalidStateEqualities) {
    int invalidRow = 0;
    int invalidCol = 0;
    final Map<State, Point2D> invalidTuples = new HashMap<>();
    for (final String invalidStateEquality : invalidStateEqualities) {
      final Point2D pos = getPositionForRowAndCol(invalidRow, invalidCol);
      invalidTuples.put(getStateForEqualityPred(invalidStateEquality),
          pos.add(ValidationPane.WIDTH / 2, 0));
      invalidCol++;
      if (invalidCol == maxPerRow - 1) {
        invalidCol = 0;
        invalidRow++;
      }
    }
    return invalidTuples;
  }

  /**
   * Use {@link FindStateCommand} to compute the state for a given string describing a state
   * equality predicate.
   */
  private State getStateForEqualityPred(final String stateEquality) {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final FindStateCommand findStateCommand =
        new FindStateCommand(stateSpace, new ClassicalB(stateEquality, FormulaExpand.EXPAND),
            false);
    stateSpace.execute(findStateCommand);
    return stateSpace.getState(findStateCommand.getStateId());
  }

  /**
   * Get the next position for a node. If the node is validated to be a negative example, add
   * ValidationPane.WIDTH / 2 afterwards.
   */
  private Point2D getPositionForRowAndCol(final int row, final int col) {
    final int offsetX = 50 + (col != 0 ? 25 : 0);
    final int offsetY = 50 + (row != 0 ? 25 : 0);
    return new Point2D(col * StateNode.WIDTH * 3 + offsetX, row * StateNode.HEIGHT + offsetY);
  }

  /**
   * Visualize an existing operation by providing valid and invalid transitions.
   */
  public Map<String, Set<TransitionNode>> visualizeOperation(
      final String operationName,
      final TransitionNodeFactory transitionNodeFactory) {
    int validRow = 0;
    int validCol = 0;

    synthesisContextService.setSynthesisType(SynthesisType.ACTION);
    synthesisContextService.setCurrentOperation(operationName);

    final VisualizeOperationCommand visualizeOperationCommand =
        new VisualizeOperationCommand(operationName, 5, 5);
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    stateSpace.execute(visualizeOperationCommand);

    ignoredIDsProperty.clear();
    ignoredIDsProperty.addAll(visualizeOperationCommand.ignoredIDsProperty().get());

    final List<VarValueTuple> validTransitionEqualities =
        visualizeOperationCommand.validTransitionEqualitiesProperty().get();
    final List<String> invalidStateEqualities =
        visualizeOperationCommand.invalidStateEqualitiesProperty().get();

    final Map<String, Set<TransitionNode>> transitionNodes = new HashMap<>();
    final Set<TransitionNode> validTransitions = new HashSet<>();
    final Set<TransitionNode> invalidTransitions = new HashSet<>();
    transitionNodes.put("valid", validTransitions);
    transitionNodes.put("invalid", invalidTransitions);

    for (final VarValueTuple varValueTuple : validTransitionEqualities) {
      validTransitions.add(
          getTransitionNodeFromTuple(validRow, validCol, varValueTuple, transitionNodeFactory));
      validCol++;
      if (validCol == maxPerRow - 1) {
        validCol = 0;
        validRow++;
      }
    }
    final Map<State, Point2D> invalidTuples =
        getStatePositionTuplesForNegativeEqualities(invalidStateEqualities);
    invalidTuples.forEach((state, pos) ->
        invalidTransitions.add(transitionNodeFactory.create(state, null, pos,
            NodeState.TENTATIVE)));
    return transitionNodes;
  }

  private TransitionNode getTransitionNodeFromTuple(
      int validRow, int validCol, final VarValueTuple varValueTuple,
      final TransitionNodeFactory transitionNodeFactory) {
    final StateSpace stateSpace = synthesisContextService.getStateSpace();
    final FindStateCommand findInputStateCommand =
        new FindStateCommand(stateSpace, new ClassicalB(varValueTuple.getVar(),
            FormulaExpand.EXPAND), false);
    stateSpace.execute(findInputStateCommand);
    final State inputState = stateSpace.getState(findInputStateCommand.getStateId());
    final FindStateCommand findOutputStateCommand =
        new FindStateCommand(stateSpace, new ClassicalB(varValueTuple.getValue(),
            FormulaExpand.EXPAND), false);
    stateSpace.execute(findOutputStateCommand);
    final int offsetX = 50 + (validCol != 0 ? 25 : 0);
    final int offsetY = 50 + (validRow != 0 ? 25 : 0);
    final Point2D pos = new Point2D(
        validCol * StateNode.WIDTH * 3 + offsetX,
        validRow * StateNode.HEIGHT + offsetY);
    final State outputState = stateSpace.getState(findOutputStateCommand.getStateId());
    return transitionNodeFactory.create(inputState, outputState, pos, NodeState.TENTATIVE);
  }

  public ListProperty<String> ignoredIDsProperty() {
    return ignoredIDsProperty;
  }
}
