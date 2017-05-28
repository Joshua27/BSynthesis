package de.hhu.stups.bsynthesis.ui.components.library;

import java.util.Objects;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A container for a library component to configure the used library for the synthesis tool. The
 * combination of component name and type is unique.
 */
public class LibraryComponent {

  private final StringProperty componentNameProperty;
  private final IntegerProperty amountProperty;
  private final LibraryComponentType libraryComponentType;

  public LibraryComponent(final String componentName,
                   final int amount,
                   final LibraryComponentType libraryComponentType) {
    this.libraryComponentType = libraryComponentType;
    componentNameProperty = new SimpleStringProperty(componentName);
    amountProperty = new SimpleIntegerProperty(amount);
  }

  public void increaseAmount() {
    amountProperty.set(amountProperty.add(1).get());
  }

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

  public String getComponentName() {
    return componentNameProperty.get();
  }

  public LibraryComponentType getLibraryComponentType() {
    return libraryComponentType;
  }

  public IntegerProperty amountProperty() {
    return amountProperty;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LibraryComponent that = (LibraryComponent) o;
    return Objects.equals(componentNameProperty, that.componentNameProperty) &&
        libraryComponentType == that.libraryComponentType;
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
