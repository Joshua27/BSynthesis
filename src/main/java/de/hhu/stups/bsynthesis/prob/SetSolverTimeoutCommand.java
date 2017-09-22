package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

public class SetSolverTimeoutCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "set_solver_timeout";
  private final String timeout;

  public SetSolverTimeoutCommand(final String timeout) {
    this.timeout = timeout;
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME).printNumber(Integer.parseInt(timeout)).closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    //
  }
}
