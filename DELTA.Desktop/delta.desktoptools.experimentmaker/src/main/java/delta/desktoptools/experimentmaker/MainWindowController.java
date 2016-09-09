package delta.desktoptools.experimentmaker;

import delta.desktoptools.library.IOHelpers;
import delta.desktoptools.library.ProgressReportListener;
import delta.desktoptools.library.SettingsHelper;
import delta.desktoptools.library.SigningInfo;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import jfx.messagebox.MessageBox;
import unipd.elia.delta.sharedlib.MathHelpers;
import unipd.elia.delta.sharedlib.PluginConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class MainWindowController implements ProgressReportListener {
    @FXML TextField txtExperimentName;
    @FXML TextField txtExperimentAuthor;
    @FXML TextField txtPackageId;
    @FXML TextArea txtExperimentDescription;
    @FXML TextField txtDeltaServerUrl;
    @FXML Button buttonBuildExperiment;
    @FXML Button btnReload;
    @FXML VBox vboxPlugins;
    @FXML TextArea txtBuildLog;
    @FXML TabPane tabPane;
    @FXML Tab tabProgress;
    @FXML RadioButton radioContinuousLogging;
    @FXML RadioButton radioLogOnlyWhenScreenOn;
    @FXML Label labelWakeUpRate;
    @FXML Label labelStrictCycles;
    @FXML Label labelSporadicCycles;
    @FXML Label labelBatteryInpact;
    @FXML Label labelPackagePrefix;
    @FXML HBox hboxLoading;
    @FXML VBox vboxPluginsMain;
    @FXML BorderPane borderPanePluginsHeader;
    @FXML GridPane gridSigningInfo;
    @FXML Button btnChoseKeystoreFile;
    @FXML TextField txtKeyAlias;
    @FXML PasswordField txtKeystorePassword;
    @FXML PasswordField txtKeyPassword;
    @FXML RadioButton radioReleaseMode;

    SigningInfo signingInfo = new SigningInfo();

    public void Initialize(){
        LoadAvailablePlugins();
        setListeners();
        setDebugValues();

        btnReload.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                LoadAvailablePlugins();
            }
        });

        btnChoseKeystoreFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select your keystore file");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("KeyStore files", "*.jks")
                    );
                    File chosenFile = fileChooser.showOpenDialog(buttonBuildExperiment.getScene().getWindow());

                    if (chosenFile != null) {
                        btnChoseKeystoreFile.setText(chosenFile.getName());
                        signingInfo.keystoreFilePath = chosenFile.getCanonicalPath();
                    }
                } catch (Exception ex) {
                    return;
                }
            }
        });

        radioReleaseMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean newVal) {
                gridSigningInfo.setDisable(!newVal);
            }
        });

        buttonBuildExperiment.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (!ExperimentConfigurationHelper.currentExperimentConfiguration.ValidateConfiguration()) {
                    MessageBox.show(buttonBuildExperiment.getScene().getWindow(),
                            "One or more fields in the Experiment Configuration section aren't filled in, or they contain incorrect values",
                            "Incorrect configuration",
                            MessageBox.ICON_ERROR | MessageBox.OK);
                    return;
                }

                if (!ExperimentConfigurationHelper.currentExperimentConfiguration.ValidatePluginsConfiguration()) {
                    MessageBox.show(buttonBuildExperiment.getScene().getWindow(),
                            "You must select at least one non-optional logging plugin for a valid experiment!",
                            "Incorrect configuration",
                            MessageBox.ICON_ERROR | MessageBox.OK);
                    return;
                }

                if (radioReleaseMode.isSelected() && !signingInfo.isValid()) {
                    MessageBox.show(buttonBuildExperiment.getScene().getWindow(),
                            "You have chosen to sign your experiment but you didn't fill in all the required information.\n\n" +
                                    "Please chose a keystore file and fill in the keystore password, key alias and key password fields",
                            "Incorrect configuration",
                            MessageBox.ICON_ERROR | MessageBox.OK);
                    return;
                }


                try {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Select output file");
                    fileChooser.setInitialFileName(ExperimentConfigurationHelper.currentExperimentConfiguration.ExperimentPackage + ".deltaexp");
                    File repoDir = new File(SettingsHelper.PATH_EXPERIMENTS_REPO);
                    if (repoDir.exists())
                        fileChooser.setInitialDirectory(repoDir.getCanonicalFile());
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("DELTA Experiment Bundle", "*.deltaexp")
                    );
                    File chosenFile = fileChooser.showSaveDialog(buttonBuildExperiment.getScene().getWindow());

                    if (chosenFile != null) {
                        switchToBuildProgress();
                        txtBuildLog.clear();
                        IOHelpers.buildExperimentAsync(
                                ExperimentConfigurationHelper.currentExperimentConfiguration,
                                SettingsHelper.PATH_ANDROID_PROJECT,
                                chosenFile.getAbsolutePath(),
                                !radioReleaseMode.isSelected(),
                                radioReleaseMode.isSelected() && signingInfo.isValid() ? signingInfo : null,
                                (new File(SettingsHelper.PATH_EXTERNAL_PLUGINS).exists() ? SettingsHelper.PATH_EXTERNAL_PLUGINS : null),
                                MainWindowController.this);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageBox.show(buttonBuildExperiment.getScene().getWindow(),
                            "Failed to start experiment build: unhandled exception\n\nMessage: " + ex.getMessage(),
                            "Fatal Error",
                            MessageBox.ICON_ERROR | MessageBox.OK);
                }
            }
        });

    }

    public void setDebugValues(){
        //TODO: Temporary...
        txtExperimentAuthor.setText("Elia Dal Santo");
        txtExperimentName.setText("Test ExperimentMaker experiment 1");
        txtPackageId.setText("experimentmaker1");
        txtExperimentDescription.setText("A test experiment to see if this tool actually works...");
        txtDeltaServerUrl.setText(SettingsHelper.DELTA_SERVICE_ENDPOINT);
    }

    public void LoadAvailablePlugins(){
        btnReload.setDisable(true);
        vboxPlugins.getChildren().clear();
        if(!(borderPanePluginsHeader.getBottom() == hboxLoading))
            borderPanePluginsHeader.setBottom(hboxLoading);
        ExperimentConfigurationHelper.currentExperimentConfiguration.Plugins.clear();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                final Map<String, List<PluginConfiguration>> availablePlugins = ExperimentConfigurationHelper.getAvailablePlugins(SettingsHelper.PATH_ANDROID_PROJECT);
                final Map<String, List<PluginConfiguration>> availableExternalPlugins = ExperimentConfigurationHelper.getAvailableExternalPlugins(SettingsHelper.PATH_EXTERNAL_PLUGINS);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        populatePlugins(availablePlugins, availableExternalPlugins);
                        btnReload.setDisable(false);
                    }
                });
            }
        });

        t.start();

   }

    private void populatePlugins(Map<String, List<PluginConfiguration>> availablePlugins,
                                 Map<String, List<PluginConfiguration>> availableExternalPlugins){

        if(availablePlugins != null) {
            for (String packageID : availablePlugins.keySet()) {
                HBox hBox = new HBox();
                Label label = new Label("From package: ");
                label.getStyleClass().add("headerLabel");
                Label labelPackage = new Label(packageID);
                labelPackage.getStyleClass().add("headerLabelPackage");
                hBox.setAlignment(Pos.TOP_CENTER);
                hBox.getChildren().addAll(label, labelPackage);

                vboxPlugins.getChildren().add(hBox);
                VBox.setMargin(hBox, new Insets(12, 0, 0, 0));

                for (PluginConfiguration deltaPluginClass : availablePlugins.get(packageID)) {
                    FXMLLoader fxmlLoader = new FXMLLoader();
                    try {
                        GridPane root = (GridPane) fxmlLoader.load(getClass().getResource("PluginClassControl.fxml").openStream());
                        PluginClassControlController mainWindowController = fxmlLoader.getController();
                        mainWindowController.Initialize(deltaPluginClass);

                        vboxPlugins.getChildren().add(root);
                        VBox.setMargin(root, new Insets(3, 0, 3, 0));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if(availableExternalPlugins != null) {
            for (String packageID : availableExternalPlugins.keySet()) {
                HBox hBox = new HBox();
                Label label = new Label("From package: ");
                label.getStyleClass().add("headerLabel");
                Label labelPackage = new Label(packageID);
                labelPackage.getStyleClass().add("headerLabelPackage");
                Label labelExternal = new Label("  [EXTERNAL/PREBUILT]");
                labelExternal.getStyleClass().add("externalLabel");
                hBox.setAlignment(Pos.TOP_CENTER);
                hBox.getChildren().addAll(label, labelPackage, labelExternal);

                vboxPlugins.getChildren().add(hBox);
                VBox.setMargin(hBox, new Insets(12, 0, 0, 0));

                for (PluginConfiguration deltaPluginClass : availableExternalPlugins.get(packageID)) {
                    FXMLLoader fxmlLoader = new FXMLLoader();
                    try {
                        GridPane root = (GridPane) fxmlLoader.load(getClass().getResource("PluginClassControl.fxml").openStream());
                        PluginClassControlController mainWindowController = fxmlLoader.getController();
                        mainWindowController.Initialize(deltaPluginClass);

                        vboxPlugins.getChildren().add(root);
                        VBox.setMargin(root, new Insets(3, 0, 3, 0));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        borderPanePluginsHeader.setBottom(null);
    }

    private void setListeners(){
        txtPackageId.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                ExperimentConfigurationHelper.currentExperimentConfiguration.ExperimentPackage = labelPackagePrefix.getText() + newValue;
            }
        });

        txtExperimentName.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                ExperimentConfigurationHelper.currentExperimentConfiguration.ExperimentName = newValue;
            }
        });

        txtExperimentAuthor.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                ExperimentConfigurationHelper.currentExperimentConfiguration.ExperimentAuthor = newValue;
            }
        });

        txtExperimentDescription.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                ExperimentConfigurationHelper.currentExperimentConfiguration.ExperimentDescription = newValue;
            }
        });

        txtDeltaServerUrl.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                ExperimentConfigurationHelper.currentExperimentConfiguration.DeltaServerUrl = newValue;
            }
        });

        radioContinuousLogging.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_val, Boolean new_val) {
                if (new_val)
                    ExperimentConfigurationHelper.currentExperimentConfiguration.SuspendOnScreenOff = false;
            }
        });

        radioLogOnlyWhenScreenOn.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old_val, Boolean new_val) {
                if (new_val)
                    ExperimentConfigurationHelper.currentExperimentConfiguration.SuspendOnScreenOff = true;
            }
        });

        txtKeyAlias.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                signingInfo.keyAlias = newValue;
            }
        });

        txtKeystorePassword.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                signingInfo.keystorePassword = newValue;
            }
        });

        txtKeyPassword.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                signingInfo.keyPassword = newValue;
            }
        });
    }

    public void recalculateExperimentRates(){
        labelBatteryInpact.setText("N/A");
        labelWakeUpRate.setText("N/A");
        labelSporadicCycles.setText("N/A");
        labelStrictCycles.setText("N/A");
        if(ExperimentConfigurationHelper.isExperimentWakelocking()){
            labelWakeUpRate.setText("Always awake! (at least one selected plugin wakelocks the device)");
        } else {
            MathHelpers.ExperimentCycles strictCycles = MathHelpers.getStrictTimerCycles(ExperimentConfigurationHelper.currentExperimentConfiguration);
            MathHelpers.ExperimentCycles sporadicCycles = MathHelpers.getSporadicCycles(ExperimentConfigurationHelper.currentExperimentConfiguration);
            int minWakeup = 0;

            if(strictCycles != null){
                String s = "[";
                for(MathHelpers.CycleValues cycleValues : strictCycles.cycle2plugins.keySet()){
                    s += "," + MathHelpers.getReadableTime(cycleValues.frequency);
                    if(minWakeup > cycleValues.frequency || minWakeup == 0)
                        minWakeup = cycleValues.frequency;
                }
                s = s.replaceFirst(",", "") + "]";
                labelStrictCycles.setText(s);
            }

            if(sporadicCycles != null){
                String s = "[";
                for(MathHelpers.CycleValues cycleValues : sporadicCycles.cycle2plugins.keySet()){
                    s += "," + MathHelpers.getReadableTime(cycleValues.frequency);
                    if(minWakeup > cycleValues.frequency || minWakeup == 0)
                        minWakeup = cycleValues.frequency;
                }
                s = s.replaceFirst(",", "") + "]";
                labelSporadicCycles.setText(s);
            }

            if(minWakeup != 0) {
                labelWakeUpRate.setText("every " + MathHelpers.getReadableTime(minWakeup));
            }
        }
    }

    private void switchToBuildProgress(){
        tabPane.getSelectionModel().select(tabProgress);
        for(Tab tab : tabPane.getTabs()){
            if(tab != tabProgress)
                tab.setDisable(true);
        }

    }

    private void reEnableTabs(){
        for(Tab tab : tabPane.getTabs()){
            tab.setDisable(false);
        }
    }

    @Override
    public void ReportProgress(final String data) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtBuildLog.appendText(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Timestamp(System.currentTimeMillis())) +
                        " - " + data + "\n");
            }
        });
    }

    @Override
    public void ReportError(final String data) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                txtBuildLog.appendText(new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(new Timestamp(System.currentTimeMillis())) +
                        " - ERROR: " + data + "\n");
            }
        });
    }

    @Override
    public void Finished(final boolean success) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if(success)
                    txtBuildLog.appendText("BUILD WAS SUCCESSFUL!!");
                else
                    txtBuildLog.appendText("BUILD FAILED!! Check the log for details.");
                reEnableTabs();
            }
        });
    }
}
