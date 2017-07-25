package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ResetSynthesisCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "reset_synthesis_context";

  private final BooleanProperty commandSucceededProperty;

  public ResetSynthesisCommand() {
    commandSucceededProperty = new SimpleBooleanProperty(false);
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME).closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    commandSucceededProperty.set(true);
  }

  public BooleanProperty commandSucceededProperty() {
    return commandSucceededProperty;
  }
}
