package com.osumania.skinbuilder.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class KeymodeTab extends ScrollPane {

    private int keyCount;

    public KeymodeTab(int keyCount) {
        this.keyCount = keyCount;
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(15));

        // --- 1. SECCIÓN: OPCIONES GENERALES ---
        TitledPane generalPane = createGeneralOptions();

        // --- 2. SECCIÓN: CONFIGURACIÓN DINÁMICA DE COLUMNAS ---
        TitledPane columnsPane = createColumnsOptions(keyCount);

        mainContainer.getChildren().addAll(generalPane, columnsPane);
        setContent(mainContainer);
    }

    private TitledPane createGeneralOptions() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        // Controles básicos
        Label hitPosLabel = new Label("HitPosition:");
        TextField hitPosField = new TextField("410");
        hitPosField.setPrefWidth(70);

        CheckBox percyCheck = new CheckBox("Activar Percy (LN Body)");

        // El Split Stages solo tiene sentido en modos de 10K en adelante
        CheckBox splitStagesCheck = new CheckBox("Split Stages (10K+)");
        splitStagesCheck.setDisable(keyCount < 10);

        // Añadir al grid (columna, fila, colspan, rowspan)
        grid.add(hitPosLabel, 0, 0);
        grid.add(hitPosField, 1, 0);
        grid.add(percyCheck, 0, 1, 2, 1);
        grid.add(splitStagesCheck, 0, 2, 2, 1);

        TitledPane pane = new TitledPane("Opciones Generales", grid);
        pane.setCollapsible(false); // Para que esté siempre abierto
        return pane;
    }

    private TitledPane createColumnsOptions(int keys) {
        // HBox principal que contendrá las X columnas alineadas en horizontal
        HBox columnsContainer = new HBox(15);
        columnsContainer.setPadding(new Insets(15));
        columnsContainer.setAlignment(Pos.CENTER_LEFT);

        // Bucle mágico: Crea la interfaz de cada columna según el keymode
        for (int i = 1; i <= keys; i++) {
            VBox colBox = new VBox(15);
            colBox.setAlignment(Pos.TOP_CENTER);
            colBox.setStyle("-fx-border-color: #d3d3d3; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #f9f9f9; -fx-background-radius: 8;");

            Label title = new Label("Columna " + i);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // --- Ancho de columna ---
            HBox widthBox = new HBox(5);
            widthBox.setAlignment(Pos.CENTER);
            Label widthLabel = new Label("Width:");
            TextField widthField = new TextField("64");
            widthField.setPrefWidth(50);
            widthBox.getChildren().addAll(widthLabel, widthField);

            // --- Color Rice (Nota normal) ---
            VBox riceBox = new VBox(5);
            riceBox.setAlignment(Pos.CENTER);
            Label riceLabel = new Label("Rice Color:");
            ColorPicker riceColor = new ColorPicker(Color.WHITE);
            riceBox.getChildren().addAll(riceLabel, riceColor);

            // --- Color LN (Nota larga) ---
            VBox lnBox = new VBox(5);
            lnBox.setAlignment(Pos.CENTER);
            Label lnLabel = new Label("LN Color:");
            // Para diferenciar un poco por defecto, alternamos colores
            Color defaultLnColor = (i % 2 == 0) ? Color.LIGHTBLUE : Color.LIGHTGOLDENRODYELLOW;
            ColorPicker lnColor = new ColorPicker(defaultLnColor);
            lnBox.getChildren().addAll(lnLabel, lnColor);

            // Montamos la "tarjeta" de la columna
            colBox.getChildren().addAll(title, widthBox, riceBox, lnBox);
            columnsContainer.getChildren().add(colBox);
        }

        // Metemos las columnas en un ScrollPane horizontal por si son 18K y no caben en pantalla
        ScrollPane scrollCols = new ScrollPane(columnsContainer);
        scrollCols.setFitToHeight(true);
        scrollCols.setVbarPolicy(ScrollBarPolicy.NEVER);
        scrollCols.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        TitledPane pane = new TitledPane("Configuración de Notas", scrollCols);
        pane.setCollapsible(false);
        return pane;
    }
}