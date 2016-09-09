package delta.desktoptools.experimentmaker;

import delta.desktoptools.library.SettingsHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        System.setProperty("javax.userAgentStylesheetUrl", "caspian");
        Constants.mainStage = primaryStage;

        FXMLLoader fxmlLoader = new FXMLLoader();

        Pane root = (Pane) fxmlLoader.load(getClass().getResource("MainWindow.fxml").openStream());

        String css = getClass().getResource("MainWindow.css").toExternalForm();
        root.getStylesheets().clear();
        root.getStylesheets().add(css);

        MainWindowController mainWindowController = fxmlLoader.getController();
        Constants.mainWindowController = mainWindowController;
        mainWindowController.Initialize();

        primaryStage.setTitle("DELTA Experiment Maker");
        primaryStage.setScene(new Scene(root, 1024, 600));

        primaryStage.show();
    }

    @Override
    public void init() {
        try {
            SettingsHelper.loadSettings(new File("delta_settings.ini"));
            SettingsHelper.loadWebServerSettings(new File("delta_settings.ini"));
        } catch (Exception e) {
            System.out.println("Failed to load setting INI file. Error: \n" + e.getMessage());
            return;
        }

    }

    public static void main(String[] args){
        launch(args);
    }
}
