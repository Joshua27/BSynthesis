package de.hhu.stups.bsynthesis.ui.components;

import de.hhu.stups.bsynthesis.ui.components.nodes.StateNode;
import de.hhu.stups.bsynthesis.ui.controller.ValidationPane;
import de.prob.statespace.Trace;

import javafx.geometry.Point2D;

import java.util.HashMap;

/**
 * Stores a map containing values needed during the initial visualization of nodes from a trace
 * derived by the model checker. We want to position the nodes without any interference. We can
 * generate new positions for valid or invalid nodes according to the current ui state using {@link
 * #getNextValidNodePosition()} or {@link #getNextInvalidNodePosition()}.
 */
public class NodesFromTracePositionGenerator {

  private final HashMap<String, Double> uiStateMap;

  private StateNode previousNode;
  private Trace previousTrace;

  public NodesFromTracePositionGenerator() {
    uiStateMap = new HashMap<>(11);
    initializeMap();
  }

  private void initializeMap() {
    final double halfWidth = ValidationPane.WIDTH / 2;
    final double nodesPerRow =
        (double) Math.round((halfWidth - StateNode.WIDTH * 2) / StateNode.WIDTH);
    final double nodesPerCol =
        (double) Math.round((ValidationPane.HEIGHT - StateNode.HEIGHT * 2) / StateNode.HEIGHT);
    uiStateMap.put("halfWidth", halfWidth);
    uiStateMap.put("nodesPerRow", nodesPerRow);
    uiStateMap.put("nodesPerCol", nodesPerCol);
    uiStateMap.put("nodeXPos", (double) Math.round(ValidationPane.WIDTH / nodesPerRow));
    uiStateMap.put("nodeYPos", (double) Math.round(ValidationPane.HEIGHT / nodesPerCol));
    setValidNodes(0.0);
    setInvalidNodes(0.0);
    uiStateMap.put("validNodesRow", 0.0);
    setValidNodesCol(0.0);
    uiStateMap.put("invalidNodesRow", 0.0);
    setInvalidNodesCol(0.0);
  }

  /**
   * Compute the position for the next valid {@link StateNode} when initializing the nodes from a
   * trace.
   */
  public Point2D getNextValidNodePosition() {
    double validNodes = uiStateMap.get("validNodes");
    double validNodesCol = uiStateMap.get("validNodesCol");
    final double tempValid = (validNodes + 1) * getNodeXPosition();
    final Point2D validNodePosition = new Point2D(
        (uiStateMap.get("validNodesRow") % 2 < 0.000001) ? getHalfWidth() - tempValid : tempValid,
        (validNodesCol + 1) * getNodeYPosition() + 50.0);
    validNodes++;
    if (validNodes > (uiStateMap.get("nodesPerCol"))) {
      setValidNodes(0.0);
      setValidNodesCol(validNodesCol + 1);
    } else {
      setValidNodes(validNodes);
    }
    return validNodePosition;
  }

  /**
   * Compute the position for the next invalid {@link StateNode} when initializing the nodes from a
   * trace.
   */
  public Point2D getNextInvalidNodePosition() {
    double invalidNodes = uiStateMap.get("invalidNodes");
    double invalidNodesCol = uiStateMap.get("invalidNodesCol");
    final double tempInvalid = (invalidNodes + 1) * getNodeXPosition();
    final Point2D invalidNodePosition = new Point2D(
        (uiStateMap.get("invalidNodesRow") % 2 < 0.000001)
            ? ValidationPane.WIDTH - tempInvalid : getHalfWidth() + tempInvalid,
        (invalidNodesCol + 1) * getNodeYPosition() + 50.0);
    invalidNodes++;
    if (invalidNodes > uiStateMap.get("nodesPerRow")) {
      setInvalidNodes(0.0);
      setInvalidNodesCol(invalidNodesCol + 1);
    } else {
      setInvalidNodes(invalidNodes);
    }
    return invalidNodePosition;
  }

  public StateNode getPreviousNode() {
    return previousNode;
  }

  public void setPreviousNode(final StateNode node) {
    previousNode = node;
  }

  public Trace getPreviousTrace() {
    return previousTrace;
  }

  public void setPreviousTrace(final Trace trace) {
    previousTrace = trace;
  }

  private double getHalfWidth() {
    return uiStateMap.get("halfWidth");
  }

  private double getNodeXPosition() {
    return uiStateMap.get("nodeXPos");
  }

  private double getNodeYPosition() {
    return uiStateMap.get("nodeYPos");
  }

  private void setValidNodes(final double value) {
    uiStateMap.put("validNodes", value);
  }

  private void setInvalidNodes(final double value) {
    uiStateMap.put("invalidNodes", value);
  }

  private void setValidNodesCol(final double value) {
    uiStateMap.put("validNodesCol", value);
  }

  private void setInvalidNodesCol(final double value) {
    uiStateMap.put("invalidNodesCol", value);
  }
}
