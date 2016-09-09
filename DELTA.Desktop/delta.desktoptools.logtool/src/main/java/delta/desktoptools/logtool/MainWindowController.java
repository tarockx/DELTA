package delta.desktoptools.logtool;

import delta.desktoptools.library.logtools.LogFileEntry;
import delta.desktoptools.library.logtools.LogWriter;
import delta.desktoptools.library.SettingsHelper;
import delta.desktoptools.library.logtools.LogsHelper;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import jfx.messagebox.MessageBox;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

public class MainWindowController {
    @FXML TreeView treeViewLogs;
    @FXML Label labelExperimentId;
    @FXML Label labelUserId;
    @FXML Label labelEntryCount;
    @FXML Label labelTimespan;
    @FXML Button buttonMergeUserLogsAndExport;
    @FXML Button buttonMergeExperimentLogsAndExport;
    @FXML Button buttonExportSingleLogfile;
    @FXML ComboBox comboLogPreview;
    @FXML TextArea txtLogPreview;
    @FXML VBox vboxLogPreview;
    @FXML RadioButton radioBinaryKeepSegmented;
    @FXML RadioButton radioBinaryMerge;

    Map<String, Map<String, List<LogFileEntry>>> allEntries;

    Map<TreeItem, LogFileEntry> logEntryTreeItem2LogEntry;
    Map<TreeItem, List<LogFileEntry>> userTreeItem2LogEntries;
    Map<TreeItem, List<List<LogFileEntry>>> experimentTreeItem2LogEntries;

    Property<Boolean> merge = new SimpleBooleanProperty();

    public void Initialize(){
        allEntries = new HashMap<>();

        logEntryTreeItem2LogEntry = new HashMap<>();
        userTreeItem2LogEntries = new HashMap<>();
        experimentTreeItem2LogEntries = new HashMap<>();

        ScanExperiments();

        //onItemSelected listener
        treeViewLogs.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<String>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<String>> observable, TreeItem<String> old_val, TreeItem<String> new_val) {
                vboxLogPreview.setDisable(true);
                TreeItem<String> selectedItem = new_val;
                if (logEntryTreeItem2LogEntry.containsKey(selectedItem)) {
                    LogFileEntry logFileEntry = logEntryTreeItem2LogEntry.get(selectedItem);
                    fillEntryInfo(logFileEntry);
                    vboxLogPreview.setDisable(false);
                } else if (userTreeItem2LogEntries.containsKey(selectedItem)) {
                    List<LogFileEntry> logFileEntries = userTreeItem2LogEntries.get(selectedItem);
                    fillUserEntryInfo(logFileEntries);
                } else if(experimentTreeItem2LogEntries.containsKey(selectedItem)){
                    fillExperimentEntryInfo(selectedItem.getValue()); //TODO: hack
                } else {
                    fillEntryInfo(null);
                }
            }
        });

        comboLogPreview.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue observableValue, String oldVal, String newVal) {
                TreeItem selectedItem = (TreeItem) treeViewLogs.getSelectionModel().getSelectedItem();
                LogFileEntry selectedEntry = logEntryTreeItem2LogEntry.get(selectedItem);
                if (selectedEntry != null) {
                    String entryText = selectedEntry.readEntry(newVal);
                    if (entryText != null)
                        txtLogPreview.setText(entryText);
                    else
                        txtLogPreview.clear();
                }
            }
        });

        merge.setValue(true);
        radioBinaryMerge.selectedProperty().bindBidirectional(merge);

        setButtonListeners();
        hideButtons();

        vboxLogPreview.setDisable(true);
    }

    private void hideButtons() {
        buttonMergeUserLogsAndExport.setVisible(false);
        buttonExportSingleLogfile.setVisible(false);
        buttonMergeExperimentLogsAndExport.setVisible(false);
    }

    private void ScanExperiments(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String logsRepo = SettingsHelper.PATH_LOG_REPOSITORY;
                final File logsRepoFile = new File(logsRepo);

                if(!logsRepoFile.exists() || !logsRepoFile.isDirectory())
                    return; //TODO: inform user

                //Load all available logs
                List<LogFileEntry> allLogEntries = LogsHelper.getAllLogEntries(logsRepoFile, true, true);//TODO: might take a while. New thread?
                for(LogFileEntry logFileEntry : allLogEntries){
                    if(!allEntries.containsKey(logFileEntry.experimentID))
                        allEntries.put(logFileEntry.experimentID, new HashMap<String, List<LogFileEntry>>());
                    Map<String, List<LogFileEntry>> userMap = allEntries.get(logFileEntry.experimentID);

                    if(!userMap.containsKey(logFileEntry.userID))
                        userMap.put(logFileEntry.userID, new LinkedList<LogFileEntry>());
                    List<LogFileEntry> logFileEntries = userMap.get(logFileEntry.userID);

                    logFileEntries.add(logFileEntry);
                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        //populate tree
                        populateTree();
                    }
                });
            }
        };
        Thread t = new Thread(r);
        t.start();

    }

    private void populateTree(){

        TreeItem<String> rootItem = new TreeItem<>("Logs Repository");
        rootItem.setExpanded(true);
        treeViewLogs.setRoot(rootItem);

        for (String experiment : allEntries.keySet()){

            TreeItem<String> experimentTreeItem = new TreeItem<>(experiment);
            rootItem.getChildren().add(experimentTreeItem);
            Map<String, List<LogFileEntry>> users = allEntries.get(experiment);

            List<List<LogFileEntry>> experimentLogEntries = new LinkedList<>();
            experimentTreeItem2LogEntries.put(experimentTreeItem, experimentLogEntries);

            //each user in the experiment
            for (String user : users.keySet()){
                TreeItem<String> userTreeItem = new TreeItem<>(user);
                userTreeItem.setExpanded(false);

                List<LogFileEntry> logFileEntries = users.get(user);
                Collections.sort(logFileEntries);
                experimentLogEntries.add(logFileEntries);
                //each log of the user
                for(LogFileEntry logFileEntry : logFileEntries){
                    TreeItem<String> logEntryTreeItem = new TreeItem<>(logFileEntry.getFileName());
                    userTreeItem.getChildren().add(logEntryTreeItem);
                    logEntryTreeItem2LogEntry.put(logEntryTreeItem, logFileEntry);
                }

                experimentTreeItem.getChildren().add(userTreeItem);
                userTreeItem2LogEntries.put(userTreeItem, logFileEntries);
            }

        }
    }


    private void setButtonListeners(){
        EventHandler<ActionEvent> buttonHandler = (new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    Button sourceButton = (Button) actionEvent.getSource();
                    DirectoryChooser fileChooser = new DirectoryChooser();
                    fileChooser.setTitle("Select output directory");
                    fileChooser.setInitialDirectory(new File(SettingsHelper.PATH_EXPERIMENTS_REPO).getCanonicalFile());
                    File chosenDir = fileChooser.showDialog(sourceButton.getScene().getWindow());
                    if (chosenDir != null && chosenDir.isDirectory()) {
                        TreeItem<String> selectedItem = (TreeItem) treeViewLogs.getSelectionModel().getSelectedItem();
                        boolean success = true;

                        //single entry
                        if (logEntryTreeItem2LogEntry.containsKey(selectedItem)) {
                            LogFileEntry logFileEntry = logEntryTreeItem2LogEntry.get(selectedItem);
                            LogWriter logWriter = new LogWriter(logFileEntry, ',');
                            success = logWriter.exportCSVs(chosenDir) && logWriter.exportBinaries(chosenDir, merge.getValue());
                        }
                        else if (userTreeItem2LogEntries.containsKey(selectedItem)){ //directory entry
                            List<LogFileEntry> logFileEntries = userTreeItem2LogEntries.get(selectedItem);
                            LogWriter logWriter = new LogWriter(logFileEntries, ',');
                            success = logWriter.exportCSVs(chosenDir) && logWriter.exportBinaries(chosenDir, merge.getValue());
                        }
                        else if(experimentTreeItem2LogEntries.containsKey(selectedItem)){
                            List<List<LogFileEntry>> logFileEntriesLists = experimentTreeItem2LogEntries.get(selectedItem);
                            for(List<LogFileEntry> entryList : logFileEntriesLists){
                                LogWriter logWriter = new LogWriter(entryList, ',');
                                success = success && logWriter.exportCSVs(chosenDir) && logWriter.exportBinaries(chosenDir, merge.getValue());
                            }
                        }

                        //check result
                        if (success) {
                            MessageBox.show(sourceButton.getScene().getWindow(),
                                    "File exported successfully to:\n\n" + chosenDir.getAbsolutePath(),
                                    "Success",
                                    MessageBox.ICON_INFORMATION | MessageBox.OK);
                        } else {
                            MessageBox.show(sourceButton.getScene().getWindow(),
                                    "Failed to export file!",
                                    "Error",
                                    MessageBox.ICON_ERROR | MessageBox.OK);
                        }


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    MessageBox.show(buttonExportSingleLogfile.getScene().getWindow(),
                            "Failed to export file: unhandled exception:\n\n" + e.getMessage(),
                            "Exception",
                            MessageBox.ICON_ERROR | MessageBox.OK);
                }
            }
        });

        buttonExportSingleLogfile.setOnAction(buttonHandler);
        buttonMergeUserLogsAndExport.setOnAction(buttonHandler);
        buttonMergeExperimentLogsAndExport.setOnAction(buttonHandler);
    }

    private void fillEntryInfo(LogFileEntry logFileEntry){
        comboLogPreview.getItems().clear();
        txtLogPreview.clear();
        hideButtons();

        if(logFileEntry == null){
            labelUserId.setText("N/A");
            labelExperimentId.setText("N/A");
            labelEntryCount.setText("N/A");
            labelTimespan.setText("N/A");
        }
        else {
            if (!logFileEntry.isLoaded())
                logFileEntry.reload();

            labelExperimentId.setText(logFileEntry.experimentID);
            labelUserId.setText(logFileEntry.userID);
            labelEntryCount.setText("[Single log file selected]");

            String startTime = new Timestamp(logFileEntry.firstTimestamp).toString();
            String endTime = new Timestamp(logFileEntry.lastTimestamp).toString();

            labelTimespan.setText(startTime + " - " + endTime);
            buttonExportSingleLogfile.setVisible(true);

            comboLogPreview.getItems().addAll(logFileEntry.availableLogs);
        }
    }

    private void fillUserEntryInfo(List<LogFileEntry> logEntries){
        comboLogPreview.getItems().clear();
        txtLogPreview.clear();
        hideButtons();

        if(logEntries == null || logEntries.size() == 0){
            labelUserId.setText("N/A");
            labelExperimentId.setText("N/A");
            labelEntryCount.setText("0");
            labelTimespan.setText("N/A");
        }
        else {
            LogFileEntry first = logEntries.get(0);
            if(!first.isLoaded())
                first.reload();

            labelExperimentId.setText(first.experimentID);
            labelUserId.setText(first.userID);
            labelEntryCount.setText(logEntries.size() + " log fragment(s)");

            String startTime = new Timestamp(first.firstTimestamp).toString();

            if(logEntries.size() == 1)
                labelTimespan.setText(startTime);
            else {
                LogFileEntry last = logEntries.get(logEntries.size() - 1);
                if(!last.isLoaded())
                    last.reload();

                String endTime = new Timestamp(last.lastTimestamp).toString();
                labelTimespan.setText(startTime + " - " + endTime);
            }

            buttonMergeUserLogsAndExport.setVisible(true);
        }
    }

    private void fillExperimentEntryInfo(String experimentID){
        comboLogPreview.getItems().clear();
        txtLogPreview.clear();
        hideButtons();

        labelUserId.setText("[multiple values]");
        labelExperimentId.setText(experimentID);
        labelEntryCount.setText(allEntries.get(experimentID).size() + " user(s)");
        labelTimespan.setText("[multiple users selected]");

        buttonMergeExperimentLogsAndExport.setVisible(true);
    }
}
