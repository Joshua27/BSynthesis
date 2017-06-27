package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class SetSolverTimeoutCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "set_solver_timeout";

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final BigInteger timeoutMs;
  private final BooleanProperty commandSucceededProperty;

  /**
   * Set the ProB Solver timeout in milliseconds.
   */
  public SetSolverTimeoutCommand(final BigInteger timeoutMs) {
    this.timeoutMs = timeoutMs;
    commandSucceededProperty = new SimpleBooleanProperty(false);
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME).printNumber(timeoutMs).closeTerm();
    logger.info("Set ProB Solver timeout calling: {}", pto);
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    commandSucceededProperty.set(true);
  }
}
