package com.osumania.skinbuilder.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainWindow {

    public void start(Stage stage) {
        stage.setTitle("osu!mania Skin Builder - v1.0");

        // Contenedor principal de pestañas
        TabPane tabPane = new TabPane();

        // Pestaña General
        Tab tabGeneral = new Tab("General");
        tabGeneral.setClosable(false);
        tabGeneral.setContent(new StackPane(new Label("Opciones Generales (Nombre, Autor, Importar/Exportar .osk)")));

        // Pestaña 4K (conectada al UI dinámico)
        Tab tab4k = new Tab("4K");
        tab4k.setClosable(false);
        tab4k.setContent(new KeymodeTab(4));

        // Pestaña 7K (conectada al UI dinámico)
        Tab tab7k = new Tab("7K");
        tab7k.setClosable(false);
        tab7k.setContent(new KeymodeTab(7));

        // Añadir las pestañas al contenedor
        tabPane.getTabs().addAll(tabGeneral, tab4k, tab7k);

        // Layout de la ventana principal
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);

        // Crear escena y dimensionar la ventana
        Scene scene = new Scene(root, 1100, 650);

        stage.setScene(scene);
        stage.show();
    }
}