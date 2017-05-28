package de.hhu.stups.bsynthesis.injector;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

// copied from https://github.com/bendisposto/prob2-ui
public class GuiceBuilderFactory implements BuilderFactory {
  private final Injector injector;
  private final BuilderFactory javafxDefaultBuilderFactory = new JavaFXBuilderFactory();

  @Inject
  public GuiceBuilderFactory(final Injector injector) {
    this.injector = injector;
  }

  @Override
  public Builder<?> getBuilder(final Class<?> type) {
    if (isGuiceResponsibleForType(type)) {
      final Object instance = injector.getInstance(type);
      return () -> instance;
    } else {
      return javafxDefaultBuilderFactory.getBuilder(type);
    }
  }

  private boolean isGuiceResponsibleForType(final Class<?> type) {
    final Binding<?> binding = injector.getExistingBinding(Key.get(type));
    return binding != null;
  }
}