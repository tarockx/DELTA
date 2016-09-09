package delta.desktoptools.logtool;

import delta.desktoptools.library.SettingsHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            SettingsHelper.loadSettings(new File("delta_settings.ini"));
            SettingsHelper.loadWebServerSettings(new File("delta_settings.ini"));
        } catch (Exception e) {
            System.out.println("Failed to load setting INI file. Could not load settings.");
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader();

        Pane root = (Pane) fxmlLoader.load(getClass().getResource("MainWindow.fxml").openStream());
        MainWindowController mainWindowController = fxmlLoader.getController();
        //Constants.mainWindowController = mainWindowController;
        mainWindowController.Initialize();

        primaryStage.setTitle("DELTA Log Tool");
        primaryStage.setScene(new Scene(root, 1024, 600));

        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
