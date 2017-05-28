package de.hhu.stups.bsynthesis.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.bsynthesis.ui.components.library.LibraryComponent;
import de.hhu.stups.bsynthesis.ui.components.library.BLibrary;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

@Singleton
public class LibraryConfiguration extends GridPane implements Initializable {

  private final BLibrary bLibrary;
  private final ObjectProperty<BLibrary> selectedLibraryComponentsProperty;

  @FXML
  @SuppressWarnings("unused")
  private TreeView<LibraryComponent> treeViewLibrary;
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
  private TreeView<LibraryComponent> treeViewSelectedLibrary;
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
  private Button btIncreaseSelectedComponentAmount;
  @FXML
  @SuppressWarnings("unused")
  private Button btDecreaseSelectedComponentAmount;
  @FXML
  @SuppressWarnings("unused")
  private Button btRemoveSelectedComponent;

  // TODO: maybe use a TreeTableView for selected library components to display the component amount

  @Inject
  public LibraryConfiguration(final FXMLLoader loader) {
    bLibrary = new BLibrary();
    bLibrary.initializeLibrary();
    selectedLibraryComponentsProperty = new SimpleObjectProperty<>(new BLibrary());

    loader.setLocation(getClass().getResource("library_configuration.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    try {
      loader.load();
    } catch (final IOException exception) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE, "Loading fxml for the synthesis library configuration failed.",
          exception);
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    initializeButtons();
    initializeTreeViews();
    initializeTreeViewLibrary();
    initializeTreeViewSelectedLibrary();
  }

  @FXML
  @SuppressWarnings("unused")
  public void increaseSelectedComponentAmount() {
    final TreeItem<LibraryComponent> treeItem =
        treeViewSelectedLibrary.getSelectionModel().getSelectedItem();
    if (treeItem != null) {
      treeItem.getValue().increaseAmount();
    }
  }

  @FXML
  @SuppressWarnings("unused")
  public void decreaseSelectedComponentAmount() {
    final TreeItem<LibraryComponent> treeItem =
        treeViewSelectedLibrary.getSelectionModel().getSelectedItem();
    if (treeItem != null) {
      treeItem.getValue().decreaseAmount();
    }
  }

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
    final LibraryComponent predicates = new LibraryComponent("Predicates", 0, null);
    final LibraryComponent sets = new LibraryComponent("Sets", 0, null);
    final LibraryComponent numbers = new LibraryComponent("Numbers", 0, null);
    final LibraryComponent relations = new LibraryComponent("Relations", 0, null);
    final LibraryComponent sequences = new LibraryComponent("Sequences", 0, null);
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
    bLibrary.getPredicates().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemPredicates.getChildren().add(treeItem));
    bLibrary.getSets().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemSets.getChildren().add(treeItem));
    bLibrary.getNumbers().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemNumbers.getChildren().add(treeItem));
    bLibrary.getRelations().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemRelations.getChildren().add(treeItem));
    bLibrary.getSequences().stream().map(TreeItem::new)
        .forEach(treeItem -> treeItemSequences.getChildren().add(treeItem));

    treeViewLibrary.setOnMouseClicked(mouseEvent -> {
      if (mouseEvent.getClickCount() == 2) {
        final TreeItem<LibraryComponent> treeItem =
            treeViewLibrary.getSelectionModel().getSelectedItem();
        if (treeItem == null) {
          return;
        }
        final LibraryComponent libraryComponent = treeItem.getValue();
        if (libraryComponent != null) {
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

    treeViewSelectedLibrary.setCellFactory(param -> new SelectedComponentCell());
  }

  /**
   * Set the buttons' {@link #btIncreaseSelectedComponentAmount}, {@link
   * #btDecreaseSelectedComponentAmount} and {@link #btRemoveSelectedComponent} disable and graphic
   * properties.
   */
  private void initializeButtons() {
    btIncreaseSelectedComponentAmount.disableProperty().bind(treeViewSelectedLibrary
        .getSelectionModel().selectedItemProperty().isNull()
        .or(cbDefaultConfiguration.selectedProperty()));
    btIncreaseSelectedComponentAmount.graphicProperty().bind(Bindings.createObjectBinding(() -> {
      final FontAwesomeIconView fontAwesomeIconView = new FontAwesomeIconView();
      fontAwesomeIconView.setGlyphName("PLUS");
      fontAwesomeIconView.setGlyphSize(10);
      return fontAwesomeIconView;
    }));
    btDecreaseSelectedComponentAmount.disableProperty().bind(treeViewSelectedLibrary
        .getSelectionModel().selectedItemProperty().isNull()
        .or(cbDefaultConfiguration.selectedProperty()));
    btDecreaseSelectedComponentAmount.graphicProperty().bind(Bindings.createObjectBinding(() -> {
      final FontAwesomeIconView fontAwesomeIconView = new FontAwesomeIconView();
      fontAwesomeIconView.setGlyphName("MINUS");
      fontAwesomeIconView.setGlyphSize(10);
      return fontAwesomeIconView;
    }));
    btRemoveSelectedComponent.disableProperty().bind(treeViewSelectedLibrary
        .getSelectionModel().selectedItemProperty().isNull()
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
  private void disableRootTreeViewSelection(final TreeView<LibraryComponent> treeView) {
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
          newValue.stream().map(TreeItem::new).collect(Collectors.toSet()));
      treeItem.setExpanded(true);
    });
  }

  public ObjectProperty<BLibrary> selectedLibraryComponentsProperty() {
    return selectedLibraryComponentsProperty;
  }


  /**
   * A custom cell that shows the component name and amount of a {@link LibraryComponent}.
   */
  private static class SelectedComponentCell extends TreeCell<LibraryComponent> {
    @Override
    protected void updateItem(final LibraryComponent item, final boolean empty) {
      super.updateItem(item, empty);
      if (isEmpty()) {
        setGraphic(null);
        setText(null);
        return;
      }
      if (getTreeItem().getValue().getLibraryComponentType() == null) {
        // root item for sectioning
        setGraphic(null);
        setText(item.componentNameProperty().get());
        return;
      }
      // selectable component item
      final HBox cellBox = new HBox();
      final Label lbComponentName = new Label(item.componentNameProperty().get());
      lbComponentName.prefWidthProperty().bind(treeViewProperty().get().widthProperty()
          .subtract(100.0));
      final Label lbAmount = new Label();
      lbAmount.textProperty().bind(item.amountProperty().asString());
      lbAmount.setAlignment(Pos.CENTER_RIGHT);
      cellBox.getChildren().addAll(lbComponentName, lbAmount);
      setGraphic(cellBox);
      setText(null);
    }
  }
}
