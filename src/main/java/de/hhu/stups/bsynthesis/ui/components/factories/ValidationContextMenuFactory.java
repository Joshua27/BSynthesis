package de.hhu.stups.bsynthesis.ui.components.factories;

import de.hhu.stups.bsynthesis.ui.components.ValidationContextMenu;
import de.hhu.stups.bsynthesis.ui.SynthesisType;

public interface ValidationContextMenuFactory {
  ValidationContextMenu create(SynthesisType synthesisType);
}
