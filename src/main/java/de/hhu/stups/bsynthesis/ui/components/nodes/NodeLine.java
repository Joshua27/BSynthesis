package de.hhu.stups.bsynthesis.ui.components.nodes;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class NodeLine extends Line {

  private final BasicNode source;
  private final BasicNode target;

  /**
   * Create the {@link Line} between the two given {@link BasicNode nodes}.
   */
  NodeLine(final BasicNode source, final BasicNode target) {
    this.source = source;
    this.target = target;
    startXProperty().bind(source.positionXProperty().add(source.widthProperty().divide(2)));
    startYProperty().bind(source.positionYProperty().add(source.heightProperty().divide(2)));
    endXProperty().bind(target.positionXProperty().add(target.widthProperty().divide(2)));
    endYProperty().bind(target.positionYProperty().add(target.heightProperty().divide(2)));
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
