package de.hhu.stups.bsynthesis.ui.components.nodes;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class NodeLine extends Line {

  private final BasicNode source;
  private final BasicNode target;

  public NodeLine(final BasicNode source, final BasicNode target) {
    this.source = source;
    this.target = target;
    startXProperty().bind(source.xPositionProperty().add(source.widthProperty().divide(2)));
    startYProperty().bind(source.yPositionProperty().add(source.heightProperty().divide(2)));
    endXProperty().bind(target.xPositionProperty().add(target.widthProperty().divide(2)));
    endYProperty().bind(target.yPositionProperty().add(target.heightProperty().divide(2)));
    setStrokeWidth(2.0);
    getStrokeDashArray().setAll(10.0, 5.0);
    setStroke(Color.web("#757575"));
  }

  public BasicNode getSource() {
    return source;
  }

  public BasicNode getTarget() {
    return target;
  }
}
