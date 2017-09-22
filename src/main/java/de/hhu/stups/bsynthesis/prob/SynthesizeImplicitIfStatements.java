package de.hhu.stups.bsynthesis.prob;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.ProBApiService;
import de.hhu.stups.bsynthesis.services.ServiceDelegator;
import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.services.UiService;
import de.hhu.stups.bsynthesis.services.ValidationPaneEvent;
import de.hhu.stups.bsynthesis.services.ValidationPaneEventType;
import de.hhu.stups.bsynthesis.ui.components.nodes.BasicNode;
import de.prob.prolog.term.CompoundPrologTerm;
import de.prob.statespace.StateSpace;
import org.fxmisc.easybind.EasyBind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class SynthesizeImplicitIfStatements {

  private final UiService uiService;
  private final SynthesisContextService synthesisContextService;
  private final List<BasicNode> initialValidExamples;
  private final List<BasicNode> processedExamples;
  private final Set<CompoundPrologTerm> synthesizedOperations;
  private final ProBApiService proBApiService;

  private int operationCounter = 1;

  /**
   * Subscribe to {@link #synthesisContextService} and restore initial examples if synthesis failed.
   */
  @Inject
  public SynthesizeImplicitIfStatements(final ServiceDelegator serviceDelegator) {
    this.uiService = serviceDelegator.uiService();
    this.synthesisContextService = serviceDelegator.synthesisContextService();
    this.proBApiService = serviceDelegator.proBApiService();

    initialValidExamples = new ArrayList<>();
    processedExamples = new ArrayList<>();
    synthesizedOperations = new HashSet<>();

    EasyBind.subscribe(synthesisContextService.synthesisRunningProperty(), isRunning -> {
      if (!isRunning && !synthesisContextService.synthesisSucceededProperty().get()
          && !synthesisContextService.synthesisSuspendedProperty().get()
          && isImplicitIfSynthesis()) {
        uiService.validationPaneEventSource()
            .push(new ValidationPaneEvent(ValidationPaneEventType.CLEAR));
        // reset the initialValidExamples if synthesis failed
        initialValidExamples.forEach(basicNode -> uiService.validationPaneEventSource().push(
            new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, basicNode)));
        processedExamples.forEach(basicNode -> uiService.validationPaneEventSource().push(
            new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, basicNode)));
        initialValidExamples.clear();
        synthesizedOperations.clear();
        processedExamples.clear();
      }
    });
  }

  /**
   * Start synthesis by implicitly considering if-statements. We ignore initial negative examples
   * here since we cannot relate the semantics between positive and negative examples. If synthesis
   * has been suspended we, of course, consider negative examples in case a precondition needs to
   * be relaxed.
   */
  public void startSynthesis(final Map<String, List<BasicNode>> examples) {
    if (initialValidExamples.isEmpty() && synthesizedOperations.isEmpty()) {
      // new start of synthesis implicitly considering if-statements
      operationCounter = 1;
      initialValidExamples.addAll(examples.get("valid"));
      processedExamples.clear();
    } else if (initialValidExamples.isEmpty()
        && !synthesisContextService.synthesisSuspendedProperty().get()) {
      // done, adapt machine code with the synthesized operations
      synthesisContextService.synthesisSucceededProperty().set(true);
      uiService.validationPaneEventSource()
          .push(new ValidationPaneEvent(ValidationPaneEventType.CLEAR));
      processedExamples.forEach(basicNode -> uiService.validationPaneEventSource().push(
          new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, basicNode)));
      final AdaptMachineCodeForOperationsCommand adaptMachineCodeForOperationsCommand =
          new AdaptMachineCodeForOperationsCommand(synthesizedOperations);
      final StateSpace stateSpace = synthesisContextService.getStateSpace();
      stateSpace.execute(adaptMachineCodeForOperationsCommand);
      EasyBind.subscribe(adaptMachineCodeForOperationsCommand.machineCodeProperty(), machineCode ->
          synthesisContextService.modifiedMachineCodeProperty().set(machineCode));
      synthesizedOperations.clear();
      processedExamples.clear();
      return;
    }

    final Map<String, List<BasicNode>> currentExamples = new HashMap<>();
    if (synthesisContextService.synthesisSuspendedProperty().get()) {
      // restart by using the examples as is if synthesis has been suspended, i.e., we also consider
      // negative examples to synthesize an appropriate precondition if necessary
      currentExamples.putAll(examples);
    } else {
      final BasicNode example = initialValidExamples.get(0);
      initialValidExamples.remove(0);
      uiService.validationPaneEventSource()
          .push(new ValidationPaneEvent(ValidationPaneEventType.CLEAR));
      uiService.validationPaneEventSource().push(
          new ValidationPaneEvent(ValidationPaneEventType.SHOW_NODE, example));
      final ArrayList<BasicNode> invalidExamples = new ArrayList<>();
      invalidExamples.addAll(processedExamples);
      currentExamples.put("valid", Collections.singletonList(example));
      currentExamples.put("invalid", invalidExamples);
      processedExamples.add(example);
      synthesisContextService.getSelectedLibraryComponents().setLibraryExpansion(1);
    }
    runSynthesisForSingleExamples(currentExamples);
  }

  private void runSynthesisForSingleExamples(final Map<String, List<BasicNode>> currentExamples) {
    // TODO: pass the other examples to check for distinguishing examples
    final StartSynthesisCommand startSynthesisCommand = new StartSynthesisCommand(
        synthesisContextService.getSelectedLibraryComponents(),
        synthesisContextService.getCurrentOperation() + String.valueOf(operationCounter),
        uiService.getCurrentVarNames(),
        synthesisContextService.synthesisSuspendedProperty().get() ? new HashSet<>()
            : synthesizedOperations, // operations already checked if synthesis has been suspended
        synthesisContextService.getSynthesisType(),
        currentExamples,
        synthesisContextService.getSolverBackend());
    proBApiService.startSynthesisEventSource().push(startSynthesisCommand);
    EasyBind.subscribe(startSynthesisCommand.synthesizedOperationProperty(), operation -> {
      if (operation == null || "none".equals(operation.getFunctor())) {
        return;
      }
      if ("done".equals(operation.getFunctor())) {
        // the example is already satisfied by a synthesized operation
        if (!initialValidExamples.isEmpty()) {
          // not succeeded yet, there are still examples to consider
          synthesisContextService.synthesisSucceededProperty().set(false);
        }
        startSynthesis(currentExamples);
        return;
      }
      synthesisContextService.synthesisSucceededProperty().set(false);
      synthesizedOperations.add(operation);
      operationCounter++;
      startSynthesis(currentExamples);
    });
  }

  private boolean isImplicitIfSynthesis() {
    return (synthesisContextService.selectedLibraryComponentsProperty().get() != null
        && synthesisContextService.selectedLibraryComponentsProperty().get()
        .considerIfStatementsProperty().get().isImplicit());
  }
}
