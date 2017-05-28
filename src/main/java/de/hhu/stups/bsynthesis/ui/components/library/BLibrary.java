package de.hhu.stups.bsynthesis.ui.components.library;

import java.util.Arrays;
import java.util.Optional;

import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * A class representing a B/Event-B library configuration used for the synthesis tool. Using {@link
 * #initializeLibrary()} we are able to initialize all default components with an amount of zero,
 * e.g. to display and select components from. Without default initialization the class can be used
 * to store the user selected library components by adding and removing from the specific set
 * properties.
 */
public class BLibrary {

  private final SetProperty<LibraryComponent> predicatesProperty;
  private final SetProperty<LibraryComponent> setsProperty;
  private final SetProperty<LibraryComponent> numbersProperty;
  private final SetProperty<LibraryComponent> relationsProperty;
  private final SetProperty<LibraryComponent> sequencesProperty;

  public BLibrary() {
    predicatesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    setsProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    numbersProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    relationsProperty = new SimpleSetProperty<>(FXCollections.observableSet());
    sequencesProperty = new SimpleSetProperty<>(FXCollections.observableSet());
  }

  /**
   * Initialize the maps of all available library components that are supported by the synthesis
   * tool.
   */
  public void initializeLibrary() {
    predicatesProperty.addAll(Arrays.asList(
        new LibraryComponent("Conjunction P & Q", 0, LibraryComponentType.PREDICATES),
        new LibraryComponent("Disjunction P or Q", 0, LibraryComponentType.PREDICATES),
        new LibraryComponent("Implication P => Q", 0, LibraryComponentType.PREDICATES),
        new LibraryComponent("Equivalence P <=> Q", 0, LibraryComponentType.PREDICATES),
        new LibraryComponent("Negation not P", 0, LibraryComponentType.PREDICATES),
        new LibraryComponent("Equality E = F", 0, LibraryComponentType.PREDICATES),
        new LibraryComponent("Inequality E /= F", 0, LibraryComponentType.PREDICATES)));
    setsProperty.addAll(Arrays.asList(
        new LibraryComponent("Element of E:S", 0, LibraryComponentType.SETS),
        new LibraryComponent("Not Element of E/:S", 0, LibraryComponentType.SETS),
        new LibraryComponent("Union S\\/T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Intersection S/\\T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Difference S-T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Cartesian S*T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Cardinality card(S)", 0, LibraryComponentType.SETS),
        new LibraryComponent("Subset Of S<:T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Not Subset Of S/<:T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Strict Subset Of S<<:T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Not Strict Subset Of S/<<:T", 0, LibraryComponentType.SETS),
        new LibraryComponent("Powerset POW(S)", 0, LibraryComponentType.SETS),
        new LibraryComponent("Powerset POW1(S)", 0, LibraryComponentType.SETS),
        new LibraryComponent("Finite Subset FIN(S)", 0, LibraryComponentType.SETS),
        new LibraryComponent("Finite Subset FIN1(S)", 0, LibraryComponentType.SETS),
        new LibraryComponent("Generalized Union union(S)", 0, LibraryComponentType.SETS),
        new LibraryComponent("Generalized Intersection inter(S)", 0, LibraryComponentType.SETS)));
    numbersProperty.addAll(Arrays.asList(
        new LibraryComponent("Natural Numbers NATURAL", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Non-Zero Natural Numbers NATURAL1", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Implementable Natural Numbers NAT", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Implementable Non-Zero Natural Numbers NAT1", 0,
            LibraryComponentType.NUMBERS),
        new LibraryComponent("Set of Integers INTEGER", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Integers (MININT..MAXINT) INT", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Minimum min(m)", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Maximum max(m)", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Addition m+n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Subtraction m-n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Multiplication m*n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Division m/n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Modulo n mod n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Greater m>n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Less m<n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Greater Equal m>=n", 0, LibraryComponentType.NUMBERS),
        new LibraryComponent("Less Equal m<=n", 0, LibraryComponentType.NUMBERS)));
    relationsProperty.addAll(Arrays.asList(
        new LibraryComponent("Domain dom(r)", 0, LibraryComponentType.RELATIONS),
        new LibraryComponent("Range ran(r)", 0, LibraryComponentType.RELATIONS),
        new LibraryComponent("Domain Restriction S<|r", 0, LibraryComponentType.RELATIONS),
        new LibraryComponent("Domain Subtraction S<<|r", 0, LibraryComponentType.RELATIONS),
        new LibraryComponent("Range Restriction r|>S", 0, LibraryComponentType.RELATIONS),
        new LibraryComponent("Range Subtraction r|>>S", 0, LibraryComponentType.RELATIONS),
        new LibraryComponent("Reflexive and Transitive Closure closure(r)", 0,
            LibraryComponentType.RELATIONS),
        new LibraryComponent("Transitive Closure closure1(r)", 0, LibraryComponentType.RELATIONS)));
    sequencesProperty.addAll(Arrays.asList(
        new LibraryComponent("Concat s^t", 0, LibraryComponentType.SEQUENCES),
        new LibraryComponent("Size size(s)", 0, LibraryComponentType.SEQUENCES),
        new LibraryComponent("Reverse rev(s)", 0, LibraryComponentType.SEQUENCES),
        new LibraryComponent("First first(s)", 0, LibraryComponentType.SEQUENCES),
        new LibraryComponent("Last last(s)", 0, LibraryComponentType.SEQUENCES),
        new LibraryComponent("Tail tail(s)", 0, LibraryComponentType.SEQUENCES),
        new LibraryComponent("Front front(s)", 0, LibraryComponentType.SEQUENCES)));
  }


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
      default:
        break;
    }
  }

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
}
