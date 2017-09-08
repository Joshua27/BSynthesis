package de.hhu.stups.bsynthesis.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.parser.BindingGenerator;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.List;

public class GetMachineOperationNamesCommand extends AbstractCommand {

  private static final String PROLOG_COMMAND_NAME = "get_machine_operation_names";
  private static final String MACHINE_OPERATION_NAMES = "MachineOperationNames";

  private final ListProperty<String> machineOperationNamesProperty;

  // TODO: is this command redundant? can we access the machine operations using getMainComponent()
  // on the statespace?

  public GetMachineOperationNamesCommand() {
    machineOperationNamesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    pto.openTerm(PROLOG_COMMAND_NAME)
        .printVariable(MACHINE_OPERATION_NAMES)
        .closeTerm();
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    BindingGenerator.getList(bindings.get(MACHINE_OPERATION_NAMES)).forEach(prologTerm ->
        machineOperationNamesProperty.add(prologTerm.getFunctor()));
  }

  public List<String> getMachineOperationNames() {
    return machineOperationNamesProperty.get();
  }

}
