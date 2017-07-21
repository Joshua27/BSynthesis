package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.services.SynthesisContextService;
import de.hhu.stups.bsynthesis.ui.Loader;
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary;
import de.hhu.stups.bsynthesis.ui.components.library.LibraryComponent;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Provides an user interface to configure the used {@link BLibrary} for synthesizing programs or
 * deciding to use the default library configuration, i.e., starting from a small library and
 * successively considering new components if necessary.
 */
@Singleton
public class LibraryConfiguration extends GridPane implements Initializable {

  private final BLibrary staticBLibrary;
  private final ObjectProperty<BLibrary> selectedLibraryComponentsProperty;
  private final SynthesisContextService synthesisContextService;

  private BooleanProperty disableComponentsButtonsProperty;

  @FXML
  @SuppressWarnings("unused")
  private TreeTableView<LibraryComponent> treeViewLibrary;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<LibraryComponent, String> treeLibraryTableColumnName;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<LibraryComponent, String> treeLibraryTableColumnSyntax;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemPredicates;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSets;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemNumbers;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemRelations;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSequences;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSubstitutions;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSelectedSubstitutions;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableView<LibraryComponent> treeViewSelectedLibrary;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<LibraryComponent, String> treeSelectedLibraryTableColumnName;
  @FXML
  @SuppressWarnings("unused")
  private TreeTableColumn<LibraryComponent, String> treeSelectedLibraryTableColumnAmount;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSelectedPredicates;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSelectedSets;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSelectedNumbers;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSelectedRelations;
  @FXML
  @SuppressWarnings("unused")
  private TreeItem<LibraryComponent> treeItemSelectedSequences;
  @FXML
  @SuppressWarnings("unused")
  private CheckBox cbDefaultConfiguration;
  @FXML
  @SuppressWarnings("unused")
  private CheckBox cbConsiderIf;
  @FXML
  @SuppressWarnings("unused")
  private Button btIncreaseSelectedComponentAmount;
  @FXML
  @SuppressWarnings("unused")
  private Button btDecreaseSelectedComponentAmount;
  @FXML
  @SuppressWarnings("unused")
  private Button btRemoveSelectedComponent;

  /**
   * Initialize the {@link #staticBLibrary} to display the available {@link LibraryComponent library
   * components} to choose from. Also set up an empty {@link BLibrary} to store the library
   * components selected by the user.
   */
  @Inject
  public LibraryConfiguration(final FXMLLoader loader,
                              final SynthesisContextService synthesisContextService) {
    this.synthesisContextService = synthesisContextService;
    staticBLibrary = new BLibrary();
    selectedLibraryComponentsProperty = new SimpleObjectProperty<>(new BLibrary());
    disableComponentsButtonsProperty = new SimpleBooleanProperty(true);

    Loader.loadFxml(loader, this, "library_configuration.fxml");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    staticBLibrary.initializeLibrary();
    selectedLibraryComponentsProperty.get().considerIfStatementsProperty()
        .bindBidirectional(cbConsiderIf.selectedProperty());

    synthesisContextService.useDefaultLibraryProperty()
        .bind(selectedLibraryComponentsProperty.get().useDefaultLibraryProperty());
    cbDefaultConfiguration.selectedProperty().bindBidirectional(
        selectedLibraryComponentsProperty.get().useDefaultLibraryProperty());
    synthesisContextService.selectedLibraryComponentsProperty()
        .bind(selectedLibraryComponentsProperty);
    synthesisContextService.useDefaultLibraryProperty()
        .bind(cbDefaultConfiguration.selectedProperty());

    initializeButtons();
    initializeTreeViews();
    initializeTreeViewLibrary();
    initializeTreeViewSelectedLibrary();
  }

  /**
   * Increase the amount of the selected library component in {@link #treeViewSelectedLibrary} by
   * one.
   */
  @FXML
  @SuppressWarnings("unused")
  public void increaseSelectedComponentAmount() {
    final TreeItem<LibraryComponent> treeItem =
        treeViewSelectedLibrary.getSelectionModel().getSelectedItem();
    if (treeItem != null) {
      treeItem.getValue().increaseAmount();
    }
  }

  /**
   * Decrease the amount of the selected library component in {@link #treeViewSelectedLibrary} by
   * one.
   */
  @FXML
  @SuppressWarnings("unused")
  public void decreaseSelectedComponentAmount() {
    final TreeItem<LibraryComponent> treeItem =
        treeViewSelectedLibrary.getSelectionModel().getSelectedItem();
    if (treeItem != null) {
      treeItem.getValue().decreaseAmount();
    }
  }

  /**
   * Remove the selected library component from {@link #treeViewSelectedLibrary}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void removeSelectedComponent() {
    final TreeItem<LibraryComponent> treeItem =
        treeViewSelectedLibrary.getSelectionModel().getSelectedItem();
    if (treeItem != null) {
      selectedLibraryComponentsProperty.get().removeLibraryComponent(treeItem.getValue());
    }
  }

  /**
   * Initialize the default library component sections in both tree views {@link #treeViewLibrary}
   * and {@link #treeViewSelectedLibrary} and set selection listeners for section items.
   */
  private void initializeTreeViews() {
    final LibraryComponent predicates = new LibraryComponent("Predicates", "", "", 0, null);
    final LibraryComponent sets = new LibraryComponent("Sets", "", "", 0, null);
    final LibraryComponent numbers = new LibraryComponent("Numbers", "", "", 0, null);
    final LibraryComponent relations = new LibraryComponent("Relations", "", "", 0, null);
    final LibraryComponent sequences = new LibraryComponent("Sequences", "", "", 0, null);
    final LibraryComponent substitutions = new LibraryComponent("Substitutions", "", "", 0, null);
    // set the sectioning tree items that are the same for both tree views
    treeItemPredicates.setValue(predicates);
    treeItemSelectedPredicates.setValue(predicates);
    treeItemSets.setValue(sets);
    treeItemSelectedSets.setValue(sets);
    treeItemNumbers.setValue(numbers);
    treeItemSelectedNumbers.setValue(numbers);
    treeItemRelations.setValue(relations);
    treeItemSelectedRelations.setValue(relations);
    treeItemSequences.setValue(sequences);
    treeItemSelectedSequences.setValue(sequences);
    treeItemSubstitutions.setValue(substitutions);
    treeItemSelectedSubstitutions.setValue(substitutions);
    treeViewLibrary.disableProperty().bind(cbDefaultConfiguration.selectedProperty());
    treeViewSelectedLibrary.disableProperty().bind(cbDefaultConfiguration.selectedProperty());
    disableRootTreeViewSelection(treeViewLibrary);
    disableRootTreeViewSelection(treeViewSelectedLibrary);
  }

  /**
   * Initialize the default library that is supported by the synthesis tool within {@link
   * #treeViewLibrary}. Set a listener on the tree view to add a component to the {@link
   * #treeViewSelectedLibrary} on double click.
   */
  private void initializeTreeViewLibrary() {
    staticBLibrary.getPredicates().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemPredicates.getChildren().add(treeItem));
    staticBLibrary.getSets().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemSets.getChildren().add(treeItem));
    staticBLibrary.getNumbers().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemNumbers.getChildren().add(treeItem));
    staticBLibrary.getRelations().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemRelations.getChildren().add(treeItem));
    staticBLibrary.getSequences().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemSequences.getChildren().add(treeItem));
    staticBLibrary.getSubstitutions().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemSubstitutions.getChildren().add(treeItem));

    treeLibraryTableColumnName.setCellValueFactory(
        param -> param.getValue().getValue().componentNameProperty());
    treeLibraryTableColumnSyntax.setCellValueFactory(
        param -> param.getValue().getValue().syntaxProperty());

    treeViewLibrary.setOnMouseClicked(mouseEvent -> {
      if (mouseEvent.getClickCount() == 2) {
        final TreeItem<LibraryComponent> treeItem =
            treeViewLibrary.getSelectionModel().getSelectedItem();
        if (treeItem == null) {
          return;
        }
        final LibraryComponent libraryComponent = treeItem.getValue();
        if (libraryComponent != null && libraryComponent.getLibraryComponentType() != null) {
          selectedLibraryComponentsProperty.get().addLibraryComponent(libraryComponent);
        }
      }
    });
  }

  /**
   * Set {@link #setUpdateComponentsListener(SetProperty, TreeItem)} for each set of components of
   * the {@link #selectedLibraryComponentsProperty} referring to the  {@link BLibrary}
   * object storing the selected library components from the user.
   */
  private void initializeTreeViewSelectedLibrary() {
    final BLibrary selectedBLibrary = selectedLibraryComponentsProperty.get();
    setUpdateComponentsListener(selectedBLibrary.predicatesProperty(),
        treeItemSelectedPredicates);
    setUpdateComponentsListener(selectedBLibrary.setsProperty(), treeItemSelectedSets);
    setUpdateComponentsListener(selectedBLibrary.numbersProperty(),
        treeItemSelectedNumbers);
    setUpdateComponentsListener(selectedBLibrary.relationsProperty(),
        treeItemSelectedRelations);
    setUpdateComponentsListener(selectedBLibrary.sequencesProperty(),
        treeItemSelectedSequences);
    setUpdateComponentsListener(selectedBLibrary.substitutionsProperty(),
        treeItemSelectedSubstitutions);

    treeSelectedLibraryTableColumnName.setCellValueFactory(
        param -> param.getValue().getValue().componentNameProperty());
    treeSelectedLibraryTableColumnAmount.setCellValueFactory(
        param -> {
          final LibraryComponent libraryComponent = param.getValue().getValue();
          if (libraryComponent.getLibraryComponentType() != null) {
            return libraryComponent.amountProperty().asString();
          }
          return new SimpleStringProperty("");
        });
    // disable buttons when a root item is clicked which is only used for sectioning the library
    treeViewSelectedLibrary.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) ->
            disableComponentsButtonsProperty.set(newValue == null
                || newValue.getValue() == null
                || newValue.getValue().getLibraryComponentType() == null));
  }

  /**
   * Set the buttons' {@link #btIncreaseSelectedComponentAmount}, {@link
   * #btDecreaseSelectedComponentAmount} and {@link #btRemoveSelectedComponent} disable and graphic
   * properties.
   */
  private void initializeButtons() {
    btIncreaseSelectedComponentAmount.disableProperty().bind(disableComponentsButtonsProperty
        .or(cbDefaultConfiguration.selectedProperty()));
    btIncreaseSelectedComponentAmount.graphicProperty().bind(Bindings.createObjectBinding(() -> {
      final FontAwesomeIconView fontAwesomeIconView = new FontAwesomeIconView();
      fontAwesomeIconView.setGlyphName("PLUS");
      fontAwesomeIconView.setGlyphSize(10);
      return fontAwesomeIconView;
    }));
    btDecreaseSelectedComponentAmount.disableProperty().bind(disableComponentsButtonsProperty
        .or(cbDefaultConfiguration.selectedProperty()));
    btDecreaseSelectedComponentAmount.graphicProperty().bind(Bindings.createObjectBinding(() -> {
      final FontAwesomeIconView fontAwesomeIconView = new FontAwesomeIconView();
      fontAwesomeIconView.setGlyphName("MINUS");
      fontAwesomeIconView.setGlyphSize(10);
      return fontAwesomeIconView;
    }));
    btRemoveSelectedComponent.disableProperty().bind(disableComponentsButtonsProperty
        .or(cbDefaultConfiguration.selectedProperty()));
    btRemoveSelectedComponent.graphicProperty().bind(Bindings.createObjectBinding(() -> {
      final FontAwesomeIconView fontAwesomeIconView = new FontAwesomeIconView();
      fontAwesomeIconView.setGlyphName("REMOVE");
      fontAwesomeIconView.setGlyphSize(10);
      return fontAwesomeIconView;
    }));
  }

  /**
   * Root items are just for sectioning, they have no library component type set and should not be
   * selectable.
   */
  private void disableRootTreeViewSelection(final TreeTableView<LibraryComponent> treeView) {
    treeView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue == null) {
            return;
          }
          if (newValue.getValue().getLibraryComponentType() == null) {
            Platform.runLater(() -> treeView.getSelectionModel().select(-1));
          }
        });
  }

  /**
   * Set a listener on a given set property of specifically typed components like PREDICATES to
   * update the {@link #treeViewSelectedLibrary}'s content as soon as a set has changed.
   */
  private void setUpdateComponentsListener(final SetProperty<LibraryComponent> componentsProperty,
                                           final TreeItem<LibraryComponent> treeItem) {
    componentsProperty.addListener((observable, oldValue, newValue) -> {
      treeItem.getChildren().clear();
      treeItem.getChildren().addAll(
          newValue.stream().map(TreeItem::new).collect(Collectors.toList()));
      treeItem.setExpanded(true);
    });
  }
}
