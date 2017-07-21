package de.hhu.stups.bsynthesis.ui.components.library;

import de.prob.prolog.output.IPrologTermOutput;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * A class representing a B/Event-B library configuration used for the synthesis tool. Using {@link
 * #initializeLibrary()} we are able to initialize all default components with an amount of zero,
 * e.g. to display and select components from. Without default initialization the class can be used
 * to store the user selected library components by adding and removing from the specific set
 * properties.
 */
public class BLibrary {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final SetProperty<LibraryComponent> predicatesProperty;
  private final SetProperty<LibraryComponent> setsProperty;
  private final SetProperty<LibraryComponent> numbersProperty;
  private final SetProperty<LibraryComponent> relationsProperty;
  private final SetProperty<LibraryComponent> sequencesProperty;
  private final SetProperty<LibraryComponent> substitutionsProperty;
  private final BooleanProperty considerIfStatementsProperty;
  private final BooleanProperty useDefaultLibraryProperty;

  /**
   * Initialize the set properties to store the {@link LibraryComponent library components} split by
   * their {@link LibraryComponentType} respectively.
   */
  public BLibrary() {
    predicatesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    setsProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    numbersProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    relationsProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    sequencesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    substitutionsProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    considerIfStatementsProperty = new SimpleBooleanProperty(false);
    useDefaultLibraryProperty = new SimpleBooleanProperty(true);
  }

  public BooleanProperty considerIfStatementsProperty() {
    return considerIfStatementsProperty;
  }

  /**
   * Initialize the maps of all available library components that are supported by the synthesis
   * tool.
   */
  public void initializeLibrary() {
    readLibraryFromFile("/library/predicates.csv", predicatesProperty,
        LibraryComponentType.PREDICATES);
    readLibraryFromFile("/library/relations.csv", relationsProperty,
        LibraryComponentType.RELATIONS);
    readLibraryFromFile("/library/sequences.csv", sequencesProperty,
        LibraryComponentType.SEQUENCES);
    readLibraryFromFile("/library/numbers.csv", numbersProperty, LibraryComponentType.NUMBERS);
    readLibraryFromFile("/library/sets.csv", setsProperty, LibraryComponentType.SETS);
    readLibraryFromFile("/library/substitutions.csv", substitutionsProperty,
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
    try (final Stream<String> stream = Files.lines(
        Paths.get(getClass().getResource(filePath).getFile()))) {
      stream.forEach(line -> {
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
   * Print the selected library components to a {@link IPrologTermOutput prolog term}.
   */
  public void printToPrologTerm(final IPrologTermOutput pto) {
    if (useDefaultLibraryProperty.get()) {
      pto.printAtom("default");
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
}
