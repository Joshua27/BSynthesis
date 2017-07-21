package de.hhu.stups.bsynthesis.prob;

import de.be4.classicalb.core.parser.ClassicalBParser;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.parserbase.ProBParseException;
import de.prob.parserbase.ProBParserBaseAdapter;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.statespace.State;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class representing a single example state either input or output. Using {@link
 * #printStateToPrologTerm(IPrologTermOutput)} we are able to print the current state as a prolog
 * list to a given {@link IPrologTermOutput}.
 */
class ExampleState {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final State state;
  private final ProBParserBaseAdapter parserBaseAdapter =
      new ProBParserBaseAdapter(new ClassicalBParser());
  private final HashMap<String, String> stateMap = new HashMap<>();
  private final Set<String> currentMachineVars;

  ExampleState(final State state, final Set<String> currentMachineVars) {
    this.state = state;
    this.currentMachineVars = currentMachineVars;
    if (state == null) {
      return;
    }
    state.getValues().forEach((evalElement, abstractEvalResult) ->
        stateMap.put(evalElement.getCode(), ((EvalResult) abstractEvalResult).getValue()));
  }

  /**
   * Print a list of tuples of machine variable name and parsed value to the given
   * {@link IPrologTermOutput}.
   */
  void printStateToPrologTerm(final IPrologTermOutput prologTerm) {
    prologTerm.openList();
    stateMap.forEach((key, value) -> {
      if (currentMachineVars.contains(key)) {
        prologTerm.openTerm(",").printAtom(key);
        try {
          prologTerm.printTerm(parserBaseAdapter.parseExpression(value, false));
        } catch (final ProBParseException parseException) {
          logger.error("Error parsing value from synthesis node.", parseException);
        }
        prologTerm.closeTerm();
      }
    });
    prologTerm.closeList();
  }

  public State getState() {
    return state;
  }

  @Override
  public String toString() {
    return state.getValues().entrySet().stream()
        .map(iEvalElementAbstractEvalResultEntry ->
            ((EvalResult) iEvalElementAbstractEvalResultEntry.getValue()).toString())
        .collect(Collectors.joining(", "));
  }
}
