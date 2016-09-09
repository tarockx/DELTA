package delta.desktoptools.experimentmaker;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import unipd.elia.delta.sharedlib.MathHelpers;
import unipd.elia.delta.sharedlib.PluginConfiguration;

import java.io.IOException;
import java.util.List;


/**
 * Created by Elia on 21/05/2015.
 */
public class PluginClassControlController {
    public PluginConfiguration pluginConfiguration;

    @FXML ComboBox comboFrequency;
    @FXML Label labelPluginName;
    @FXML Label labelClassID;
    @FXML Label labelDescription;
    @FXML Label labelDeveloperDescription;
    @FXML CheckBox chkLog;
    @FXML GridPane gridMain;
    @FXML CheckBox chkAllowOptOut;
    @FXML Label labelRequiresRoot;
    @FXML Label labelRequiresWakelock;
    @FXML Label labelEventDrivenPlugin;
    @FXML Button buttonSettings;

    Stage optionsStage = null;

    public void Initialize(final PluginConfiguration pluginClass){
        this.pluginConfiguration = pluginClass;

        gridMain.getStyleClass().add("mainPluginGrid");
        gridMain.getStyleClass().add("logDisabled");

        labelEventDrivenPlugin.setVisible(!pluginClass.supportsPolling);

        labelPluginName.setText(pluginClass.PluginName);
        labelClassID.setText(pluginClass.PluginClassQualifiedName);
        labelDescription.setText(pluginClass.PluginDescription);
        labelDeveloperDescription.setText(pluginClass.DeveloperDescription);

        labelRequiresRoot.setTooltip(new Tooltip("This plugin requires a device with superuser privileges ('rooted' device)"));
        labelRequiresRoot.setVisible(pluginClass.RequiresRoot);
        labelRequiresWakelock.setTooltip(new Tooltip("This plugin will wakelock the device. This means that when the experiment is running the device's CPU will always be awake, even if the screen is off. This results in a higher overall battery drain while running the experiment"));
        labelRequiresWakelock.setVisible(pluginClass.RequiresWakelock);

        Tooltip tooltip = new Tooltip("If this is selected, the user will have the ability to optionally disable logging from this particular plugin.");
        chkAllowOptOut.setTooltip(tooltip);

        buttonSettings.setDisable(true);

        setupComboBox();

        chkLog.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue)
                    ExperimentConfigurationHelper.currentExperimentConfiguration.AddPlugin(pluginConfiguration);
                else
                    ExperimentConfigurationHelper.currentExperimentConfiguration.RemovePlugin(pluginConfiguration);

                pluginConfiguration.IsEnabled = newValue;
                comboFrequency.setDisable(!newValue);
                chkAllowOptOut.setDisable(!newValue);
                buttonSettings.setDisable(!newValue || pluginClass.Options == null || pluginClass.Options.size() == 0);

                gridMain.getStyleClass().clear();
                gridMain.getStyleClass().add("mainPluginGrid");
                if (newValue) {
                    gridMain.getStyleClass().add("logEnabled");
                } else {
                    gridMain.getStyleClass().add("logDisabled");
                }

                Constants.mainWindowController.recalculateExperimentRates();
                //gridMain.setStyle(newValue ? "-fx-background-color: #a8ff9f;" : "-fx-background-color: transparent;");
            }
        });

        chkAllowOptOut.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                pluginConfiguration.AllowOptOut = newValue;
            }
        });



        if(pluginClass.Options != null && pluginClass.Options.size() > 0){
            try {
                FXMLLoader fxmlLoader = new FXMLLoader();
                final VBox optionsControl = (VBox) fxmlLoader.load(getClass().getResource("OptionsControl.fxml").openStream());
                OptionsControlController optionsController = fxmlLoader.getController();
                optionsController.Initialize(pluginClass);

                buttonSettings.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                            if (optionsStage == null) {
                                optionsStage = new Stage();
                                optionsStage.setScene(new Scene(optionsControl));
                                optionsStage.setTitle("Plugin settings");
                                optionsStage.initModality(Modality.WINDOW_MODAL);
                                optionsStage.initOwner(Constants.mainStage.getScene().getWindow());
                            }
                            optionsStage.show();

                    }
                });
            } catch (IOException e) {
            e.printStackTrace();
            }
        }
    }

    private void setupComboBox(){
        if(!pluginConfiguration.supportsPolling) {
            comboFrequency.setVisible(false);
            return;
        }

        List<Integer> frequencies = MathHelpers.getFrequencies(pluginConfiguration.MinPollingFrequency);

        comboFrequency.setCellFactory(
                new Callback<ListView<Integer>, ListCell<Integer>>() {
                    @Override
                    public ListCell<Integer> call(ListView<Integer> param) {
                        final ListCell<Integer> cell = new ListCell<Integer>() {
                            @Override
                            public void updateItem(Integer item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null) {
                                    setText(MathHelpers.getReadableTime(item));
                                } else {
                                    setText(null);
                                }
                            }
                        };
                        return cell;
                    }
                });

        comboFrequency.getItems().addAll(
                frequencies
        );

        comboFrequency.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue ov, Integer oldValue, Integer newValue) {
                pluginConfiguration.PollingFrequency = newValue;
                Constants.mainWindowController.recalculateExperimentRates();
            }
        });
        if(pluginConfiguration.supportsPolling){
            comboFrequency.setValue(frequencies.get(0));
            comboFrequency.setDisable(true);
        }
    }
}
