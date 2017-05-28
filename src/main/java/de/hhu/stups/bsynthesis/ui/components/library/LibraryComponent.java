package de.hhu.stups.bsynthesis.ui.components.library;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

/**
 * A container for a library component to configure the used library for the synthesis tool. The
 * combination of component name and type is unique.
 */
public class LibraryComponent {

  private final StringProperty componentNameProperty;
  private final StringProperty syntaxProperty;
  private final IntegerProperty amountProperty;
  private final LibraryComponentType libraryComponentType;

  /**
   * Initialize the library component.
   *
   * @param componentName        The name of the component as a String.
   * @param amount               The amount how many times this operator should be used a an int
   *                             value.
   * @param libraryComponentType The component's {@link LibraryComponentType}.
   */
  public LibraryComponent(final String componentName,
                          final String syntax,
                          final int amount,
                          final LibraryComponentType libraryComponentType) {
    this.libraryComponentType = libraryComponentType;
    componentNameProperty = new SimpleStringProperty(componentName);
    syntaxProperty = new SimpleStringProperty(syntax);
    amountProperty = new SimpleIntegerProperty(amount);
  }

  public void increaseAmount() {
    amountProperty.set(amountProperty.add(1).get());
  }

  /**
   * Decrease the amount the library component should be used by one.
   */
  public void decreaseAmount() {
    final int newValue = amountProperty.subtract(1).get();
    if (newValue < 0) {
      amountProperty.set(0);
      return;
    }
    amountProperty.set(newValue);
  }

  void reset() {
    amountProperty.set(0);
  }

  public StringProperty componentNameProperty() {
    return componentNameProperty;
  }

  public StringProperty syntaxProperty() {
    return syntaxProperty;
  }

  public LibraryComponentType getLibraryComponentType() {
    return libraryComponentType;
  }

  public IntegerProperty amountProperty() {
    return amountProperty;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    LibraryComponent that = (LibraryComponent) obj;
    return Objects.equals(componentNameProperty, that.componentNameProperty)
        && libraryComponentType == that.libraryComponentType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentNameProperty, libraryComponentType);
  }

  @Override
  public String toString() {
    return componentNameProperty.get();
  }
}
