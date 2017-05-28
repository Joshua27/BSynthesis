package de.hhu.stups.bsynthesis.ui.components.factories;

import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.hhu.stups.bsynthesis.ui.components.nodes.NodeContextMenu;

public interface NodeContextMenuFactory {
  NodeContextMenu create(BasicNode parent);
}
