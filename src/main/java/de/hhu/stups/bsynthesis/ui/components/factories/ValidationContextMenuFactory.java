package de.hhu.stups.bsynthesis.ui.components.factories;

import de.hhu.stups.bsynthesis.ui.SynthesisType;
import de.hhu.stups.bsynthesis.ui.components.ValidationContextMenu;

public interface ValidationContextMenuFactory {
  ValidationContextMenu create(SynthesisType synthesisType);
}
