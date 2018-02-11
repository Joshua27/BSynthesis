package de.hhu.stups.bsynthesis.ui.components.library;

import de.prob.prolog.output.IPrologTermOutput;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * A class representing a B/Event-B library configuration used for the synthesis tool. Using {@link
 * #initializeLibrary()} we are able to initialize all default components with an amount of zero,
 * e.g. to display and select components from. Without default initialization the class can be used
 * to store the user selected library components by adding and removing from the specific set
 * properties. In case {@link #useDefaultLibraryProperty} is true we send the current
 * {@link #defaultLibraryExpansionProperty} to the Prolog backend instead of an explicit list of
 * components. Then, an appropriate default library configuration is generated in Prolog (see
 * library_setup.pl).
 */
public class BLibrary {

  private static final int MAXIMUM_LIBRARY_EXPANSION = 10;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final SetProperty<LibraryComponent> predicatesProperty =
      new SimpleSetProperty<>(FXCollections.observableSet());
  private final SetProperty<LibraryComponent> setsProperty =
      new SimpleSetProperty<>(FXCollections.observableSet());
  private final SetProperty<LibraryComponent> numbersProperty =
      new SimpleSetProperty<>(FXCollections.observableSet());
  private final SetProperty<LibraryComponent> relationsProperty =
      new SimpleSetProperty<>(FXCollections.observableSet());
  private final SetProperty<LibraryComponent> sequencesProperty =
      new SimpleSetProperty<>(FXCollections.observableSet());
  private final SetProperty<LibraryComponent> substitutionsProperty =
      new SimpleSetProperty<>(FXCollections.observableSet());
  private final ObjectProperty<ConsiderIfType> considerIfStatementsProperty;
  private final BooleanProperty useDefaultLibraryProperty;
  private final BooleanProperty doNotUseConstantsProperty;
  private final IntegerProperty defaultLibraryExpansionProperty;
  private final IntegerProperty solverTimeOutProperty;

  /**
   * Initialize library settings properties.
   */
  public BLibrary() {
    considerIfStatementsProperty = new SimpleObjectProperty<>(ConsiderIfType.NONE);
    useDefaultLibraryProperty = new SimpleBooleanProperty(true);
    defaultLibraryExpansionProperty = new SimpleIntegerProperty(1);
    solverTimeOutProperty = new SimpleIntegerProperty();
    doNotUseConstantsProperty = new SimpleBooleanProperty(false);
  }

  /**
   * Deep copy of a given {@link BLibrary}.
   */
  public BLibrary(final BLibrary library) {
    solverTimeOutProperty = new SimpleIntegerProperty();
    predicatesProperty.addAll(library.getPredicates());
    setsProperty.addAll(library.getSets());
    numbersProperty.addAll(library.getNumbers());
    relationsProperty.addAll(library.getRelations());
    sequencesProperty.addAll(library.getSequences());
    substitutionsProperty.addAll(library.getSubstitutions());
    considerIfStatementsProperty = new SimpleObjectProperty<>(
        library.considerIfStatementsProperty.get());
    useDefaultLibraryProperty = new SimpleBooleanProperty(
        library.useDefaultLibraryProperty.get());
    defaultLibraryExpansionProperty = new SimpleIntegerProperty(
        library.getLibraryExpansion());
    doNotUseConstantsProperty = new SimpleBooleanProperty(library.doNotUseConstantsProperty.get());
  }

  public ObjectProperty<ConsiderIfType> considerIfStatementsProperty() {
    return considerIfStatementsProperty;
  }

  /**
   * Initialize the maps of all available library components that are supported by the synthesis
   * tool.
   */
  public void initializeLibrary() {
    readLibraryFromFile("/library/predicates.csv",
        predicatesProperty,
        LibraryComponentType.PREDICATES);
    readLibraryFromFile("/library/relations.csv",
        relationsProperty,
        LibraryComponentType.RELATIONS);
    readLibraryFromFile("/library/sequences.csv",
        sequencesProperty,
        LibraryComponentType.SEQUENCES);
    readLibraryFromFile("/library/numbers.csv",
        numbersProperty, LibraryComponentType.NUMBERS);
    readLibraryFromFile("/library/sets.csv",
        setsProperty, LibraryComponentType.SETS);
    readLibraryFromFile("/library/substitutions.csv",
        substitutionsProperty,
        LibraryComponentType.SUBSTITUTIONS);
  }

  /**
   * Read library components from a .csv file following the line format:
   * ComponentName,InternalName,ComponentSyntax
   * E.g.: Natural Numbers,nat,NAT
   */
  private void readLibraryFromFile(final String filePath,
                                   final SetProperty<LibraryComponent> setProperty,
                                   final LibraryComponentType componentType) {
    try (final BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(getClass().getResourceAsStream(filePath), "UTF-8"))) {
      bufferedReader.lines().forEach(line -> {
        final String[] splitLine = line.split(",");
        setProperty.add(new LibraryComponent(splitLine[0], splitLine[1], splitLine[2], 0,
            componentType));
      });
    } catch (final IOException exception) {
      logger.error("Error reading predicates from file.", exception);
    }
  }

  /**
   * Remove a {@link LibraryComponent} from its corresponding set property of user selectable
   * library components.
   */
  public void removeLibraryComponent(final LibraryComponent libraryComponent) {
    switch (libraryComponent.getLibraryComponentType()) {
      case PREDICATES:
        predicatesProperty.remove(libraryComponent);
        break;
      case SETS:
        setsProperty.remove(libraryComponent);
        break;
      case NUMBERS:
        numbersProperty.remove(libraryComponent);
        break;
      case RELATIONS:
        relationsProperty.remove(libraryComponent);
        break;
      case SEQUENCES:
        sequencesProperty.remove(libraryComponent);
        break;
      case SUBSTITUTIONS:
        substitutionsProperty.remove(libraryComponent);
        break;
      default:
        break;
    }
  }

  /**
   * Add a {@link LibraryComponent} to its corresponding set property of user selectable
   * library components.
   */
  public void addLibraryComponent(final LibraryComponent libraryComponent) {
    switch (libraryComponent.getLibraryComponentType()) {
      case PREDICATES:
        addComponentOrIncreaseAmount(predicatesProperty, libraryComponent);
        break;
      case SETS:
        addComponentOrIncreaseAmount(setsProperty, libraryComponent);
        break;
      case NUMBERS:
        addComponentOrIncreaseAmount(numbersProperty, libraryComponent);
        break;
      case RELATIONS:
        addComponentOrIncreaseAmount(relationsProperty, libraryComponent);
        break;
      case SEQUENCES:
        addComponentOrIncreaseAmount(sequencesProperty, libraryComponent);
        break;
      case SUBSTITUTIONS:
        addComponentOrIncreaseAmount(substitutionsProperty, libraryComponent);
        break;
      default:
        break;
    }
  }

  /**
   * Add a component to the library defined by the component set property if it does not already
   * exist.
   */
  private void addComponentOrIncreaseAmount(final SetProperty<LibraryComponent> componentsProperty,
                                            final LibraryComponent libraryComponent) {
    final Optional<LibraryComponent> optionalLibraryComponent = componentsProperty.stream()
        .filter(libraryComponent::equals).findFirst();
    if (!optionalLibraryComponent.isPresent()) {
      componentsProperty.add(libraryComponent);
      libraryComponent.reset();
      libraryComponent.increaseAmount();
    }
  }

  /**
   * Print the selected library components to a {@link IPrologTermOutput prolog term} or, for
   * instance, default:1 for a default library configuration at level 1 of its predefined
   * expansions.
   */
  public void printToPrologTerm(final IPrologTermOutput pto) {
    if (useDefaultLibraryProperty.get()) {
      pto.openTerm(":")
          .printAtom("default")
          .printNumber(defaultLibraryExpansionProperty.get())
          .closeTerm();
      return;
    }
    pto.openList();
    pto.openTerm("predicates").openList();
    getPredicates().forEach(libraryComponent -> libraryComponent.printToPrologList(pto));
    pto.closeList().closeTerm();
    pto.openTerm("numbers").openList();
    getNumbers().forEach(libraryComponent -> libraryComponent.printToPrologList(pto));
    pto.closeList().closeTerm();
    pto.openTerm("relations").openList();
    getRelations().forEach(libraryComponent -> libraryComponent.printToPrologList(pto));
    pto.closeList().closeTerm();
    pto.openTerm("sequences").openList();
    getSequences().forEach(libraryComponent -> libraryComponent.printToPrologList(pto));
    pto.closeList().closeTerm();
    pto.openTerm("sets").openList();
    getSets().forEach(libraryComponent -> libraryComponent.printToPrologList(pto));
    pto.closeList().closeTerm();
    pto.openTerm("substitutions").openList();
    getSubstitutions().forEach(libraryComponent -> libraryComponent.printToPrologList(pto));
    pto.closeList().closeTerm();
    pto.closeList();
  }

  public BooleanProperty useDefaultLibraryProperty() {
    return useDefaultLibraryProperty;
  }

  public BooleanProperty doNotUseConstantsProperty() {
    return doNotUseConstantsProperty;
  }

  /**
   * Return true if there are no selected library components.
   */
  public boolean isEmpty() {
    return predicatesProperty.isEmpty()
        && setsProperty.isEmpty()
        && numbersProperty.isEmpty()
        && sequencesProperty.isEmpty()
        && relationsProperty.isEmpty()
        && substitutionsProperty.isEmpty();
  }

  /**
   * Expand the library by setting {@link #defaultLibraryExpansionProperty} if we have not reached
   * the predefined {@link #MAXIMUM_LIBRARY_EXPANSION maximum amount of library expansions}.
   */
  public boolean expandDefaultLibrary() {
    final int libraryExpansion = defaultLibraryExpansionProperty.get();
    if (libraryExpansion >= MAXIMUM_LIBRARY_EXPANSION || !useDefaultLibraryProperty.get()) {
      defaultLibraryExpansionProperty.setValue(1);
      return false;
    }
    defaultLibraryExpansionProperty.setValue(libraryExpansion + 1);
    return true;
  }

  public IntegerProperty defaultLibraryExpansionProperty() {
    return defaultLibraryExpansionProperty;
  }

  public IntegerProperty solverTimeOutProperty() {
    return solverTimeOutProperty;
  }

  public SetProperty<LibraryComponent> predicatesProperty() {
    return predicatesProperty;
  }

  public SetProperty<LibraryComponent> setsProperty() {
    return setsProperty;
  }

  public SetProperty<LibraryComponent> numbersProperty() {
    return numbersProperty;
  }

  public SetProperty<LibraryComponent> relationsProperty() {
    return relationsProperty;
  }

  public SetProperty<LibraryComponent> sequencesProperty() {
    return sequencesProperty;
  }

  public SetProperty<LibraryComponent> substitutionsProperty() {
    return substitutionsProperty;
  }

  public ObservableSet<LibraryComponent> getPredicates() {
    return predicatesProperty.get();
  }

  public ObservableSet<LibraryComponent> getSets() {
    return setsProperty.get();
  }

  public ObservableSet<LibraryComponent> getNumbers() {
    return numbersProperty.get();
  }

  public ObservableSet<LibraryComponent> getRelations() {
    return relationsProperty.get();
  }

  public ObservableSet<LibraryComponent> getSequences() {
    return sequencesProperty.get();
  }

  public ObservableSet<LibraryComponent> getSubstitutions() {
    return substitutionsProperty.get();
  }

  public int getLibraryExpansion() {
    return defaultLibraryExpansionProperty.get();
  }

  public void setLibraryExpansion(final int libraryExpansion) {
    defaultLibraryExpansionProperty().set(libraryExpansion);
  }

  public Integer getSolverTimeOut() {
    return solverTimeOutProperty.get();
  }
}
