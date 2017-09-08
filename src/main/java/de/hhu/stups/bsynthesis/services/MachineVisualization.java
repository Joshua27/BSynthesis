package de.hhu.stups.bsynthesis.services;

/**
 * A wrapper class to visualize existing behavior either of the invariants or a specific machine
 * operation.
 */
public class MachineVisualization {

  private final VisualizationType visualizationType;
  private final String operationName;

  public MachineVisualization() {
    this.visualizationType = VisualizationType.INVARIANT;
    this.operationName = null;
  }

  public MachineVisualization(final VisualizationType visualizationType,
                              final String operationName) {
    this.visualizationType = visualizationType;
    this.operationName = operationName;
  }

  VisualizationType getVisualizationType() {
    return visualizationType;
  }

  String getOperationName() {
    return operationName;
  }
}
