package com.osumania.skinbuilder;

import com.osumania.skinbuilder.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainWindow mainWindow = new MainWindow();
        mainWindow.start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}