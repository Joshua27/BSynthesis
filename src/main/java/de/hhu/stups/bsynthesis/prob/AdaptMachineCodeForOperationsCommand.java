package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.prolog.term.PrologTerm;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Set;

public class AdaptMachineCodeForOperationsCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "adapt_machine_code_for_operations_";
  private static final String MACHINE_CODE = "NewMachineCode";

  private final Set<CompoundPrologTerm> operations;
  private StringProperty machineCodeProperty = new SimpleStringProperty();

  AdaptMachineCodeForOperationsCommand(final Set<CompoundPrologTerm> operations) {
    this.operations = operations;
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME).openList();
    operations.forEach(compoundPrologTerm -> pto.printTerm(compoundPrologTerm.getArgument(2)));
    pto.closeList().printVariable(MACHINE_CODE).closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    machineCodeProperty.set(bindings.get(MACHINE_CODE).getFunctor());
  }

  StringProperty machineCodeProperty() {
    return machineCodeProperty;
  }
}
