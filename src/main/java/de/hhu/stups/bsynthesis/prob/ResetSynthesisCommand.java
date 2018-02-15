package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

/**
 * A command to retract the asserted synthesis context on a {@link de.prob.statespace.StateSpace}.
 */
public class ResetSynthesisCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "reset_synthesis_context_";

  public ResetSynthesisCommand() {
    //
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME).closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    //
  }
}
