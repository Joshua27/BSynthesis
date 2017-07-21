package de.hhu.stups.bsynthesis.prob;

import static de.hhu.stups.bsynthesis.prob.ExamplesToProlog.getInputOutputExamples;
import static de.hhu.stups.bsynthesis.prob.ExamplesToProlog.printList;

import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class GetViolatingVarsFromExamplesCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "get_invariant_violating_vars_from_examples";
  private static final String VIOLATING_VAR_NAMES = "ViolatingVarNames";

  private final Set<InputOutputExample> validExamples;
  private final Set<InputOutputExample> invalidExamples;
  private final SetProperty<String> violatingVarNamesProperty;

  /**
   * Create {@link InputOutputExample}s from the given {@link BasicNode}s and initialize
   * {@link #violatingVarNamesProperty}.
   */
  public GetViolatingVarsFromExamplesCommand(final List<BasicNode> validExamples,
                                             final List<BasicNode> invalidExamples,
                                             final Set<String> machineVarNames) {
    this.validExamples = getInputOutputExamples(validExamples, machineVarNames);
    this.invalidExamples = getInputOutputExamples(invalidExamples, machineVarNames);
    violatingVarNamesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME);
    printList(pto, validExamples);
    printList(pto, invalidExamples);
    pto.printVariable(VIOLATING_VAR_NAMES).closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    final PrologTerm resultTerm = bindings.get(VIOLATING_VAR_NAMES);
    IntStream.range(1, resultTerm.getArity() + 1).forEach(value ->
        violatingVarNamesProperty.add(resultTerm.getArgument(value).getFunctor()));
  }

  public ObservableSet<String> getViolatingVarNames() {
    return violatingVarNamesProperty.get();
  }
}
