package com.osumania.skinbuilder.ui;

import com.osumania.skinbuilder.core.ManiaKeyConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.function.IntConsumer;

public class KeymodeTab extends ScrollPane {

    private final ManiaKeyConfig maniaKeyConfig;
    private final int keyCount;
    private final PreviewCanvas previewCanvas = new PreviewCanvas();

    public KeymodeTab(ManiaKeyConfig maniaKeyConfig) {
        this.maniaKeyConfig = maniaKeyConfig;
        this.keyCount = maniaKeyConfig.getKeys();

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        VBox controlsContainer = new VBox(20);
        controlsContainer.setPadding(new Insets(15));

        TitledPane generalPane = createGeneralOptions();
        TitledPane columnsPane = createColumnsOptions();
        controlsContainer.getChildren().addAll(generalPane, columnsPane);

        StackPane previewPane = new StackPane(previewCanvas);
        previewPane.setPadding(new Insets(15));
        previewPane.setMinWidth(460);
        previewPane.setPrefWidth(480);
        previewPane.setStyle("-fx-background-color: #111318;");


        BorderPane mainContainer = new BorderPane();
        mainContainer.setLeft(controlsContainer);
        mainContainer.setCenter(previewPane);
        BorderPane.setMargin(controlsContainer, new Insets(0, 0, 0, 0));
        HBox.setHgrow(previewPane, Priority.ALWAYS);

        setContent(mainContainer);
        previewCanvas.drawPreview(maniaKeyConfig);
    }

    private TitledPane createGeneralOptions() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        Label hitPosLabel = new Label("HitPosition:");
        TextField hitPosField = new TextField(String.valueOf(maniaKeyConfig.getHitPosition()));
        hitPosField.setPrefWidth(70);
        bindIntegerField(hitPosField, maniaKeyConfig.getHitPosition(), 0, maniaKeyConfig::setHitPosition);

        Label percySizeLabel = new Label("Percy Size (px):");
        Spinner<Integer> percySizeSpinner = new Spinner<>();
        percySizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0,
                400,
                maniaKeyConfig.getPercySize()
        ));
        percySizeSpinner.setEditable(true);
        percySizeSpinner.setPrefWidth(90);
        percySizeSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            maniaKeyConfig.setPercySize(newValue == null ? 0 : newValue);
            refreshPreview();
        });
        percySizeSpinner.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitPercySpinnerValue(percySizeSpinner);
            }
        });
        percySizeSpinner.getEditor().setOnAction(event -> commitPercySpinnerValue(percySizeSpinner));

        Label percyShapeLabel = new Label("Percy Shape:");
        ComboBox<ManiaKeyConfig.PercyShape> percyShapeCombo = new ComboBox<>();
        percyShapeCombo.getItems().setAll(ManiaKeyConfig.PercyShape.values());
        percyShapeCombo.setValue(maniaKeyConfig.getPercyShape());
        percyShapeCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            maniaKeyConfig.setPercyShape(newValue);
            refreshPreview();
        });

        CheckBox splitStagesCheck = new CheckBox("Split Stages (10K+)");
        splitStagesCheck.setSelected(maniaKeyConfig.isSplitStages());
        splitStagesCheck.setDisable(keyCount < 10);
        splitStagesCheck.selectedProperty().addListener((obs, oldValue, newValue) -> {
            maniaKeyConfig.setSplitStages(newValue);
            refreshPreview();
        });

        grid.add(hitPosLabel, 0, 0);
        grid.add(hitPosField, 1, 0);
        grid.add(percySizeLabel, 0, 1);
        grid.add(percySizeSpinner, 1, 1);
        grid.add(percyShapeLabel, 0, 2);
        grid.add(percyShapeCombo, 1, 2);
        grid.add(splitStagesCheck, 0, 3, 2, 1);

        TitledPane pane = new TitledPane("Opciones Generales", grid);
        pane.setCollapsible(false);
        return pane;
    }

    private TitledPane createColumnsOptions() {
        HBox columnsContainer = new HBox(15);
        columnsContainer.setPadding(new Insets(15));
        columnsContainer.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < keyCount; i++) {
            ManiaKeyConfig.ColumnConfig columnConfig = maniaKeyConfig.getColumn(i);

            VBox colBox = new VBox(15);
            colBox.setAlignment(Pos.TOP_CENTER);
            colBox.setStyle("-fx-border-color: #d3d3d3; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #f9f9f9; -fx-background-radius: 8;");

            Label title = new Label("Columna " + (i + 1));
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            HBox widthBox = new HBox(5);
            widthBox.setAlignment(Pos.CENTER);
            Label widthLabel = new Label("Width:");
            TextField widthField = new TextField(String.valueOf(columnConfig.columnWidth));
            widthField.setPrefWidth(50);
            bindIntegerField(widthField, columnConfig.columnWidth, 1, value -> columnConfig.columnWidth = value);
            widthBox.getChildren().addAll(widthLabel, widthField);

            VBox riceBox = new VBox(5);
            riceBox.setAlignment(Pos.CENTER);
            Label riceLabel = new Label("Rice Color:");
            javafx.scene.control.ColorPicker riceColor = new javafx.scene.control.ColorPicker(toFxColor(columnConfig.riceColor));
            riceColor.valueProperty().addListener((obs, oldValue, newValue) -> {
                columnConfig.riceColor = toAwtColor(newValue);
                refreshPreview();
            });
            riceBox.getChildren().addAll(riceLabel, riceColor);

            VBox lnBox = new VBox(5);
            lnBox.setAlignment(Pos.CENTER);
            Label lnLabel = new Label("LN Color:");
            javafx.scene.control.ColorPicker lnColor = new javafx.scene.control.ColorPicker(toFxColor(columnConfig.lnColor));
            lnColor.valueProperty().addListener((obs, oldValue, newValue) -> {
                columnConfig.lnColor = toAwtColor(newValue);
                refreshPreview();
            });
            lnBox.getChildren().addAll(lnLabel, lnColor);

            colBox.getChildren().addAll(title, widthBox, riceBox, lnBox);
            columnsContainer.getChildren().add(colBox);
        }

        ScrollPane scrollCols = new ScrollPane(columnsContainer);
        scrollCols.setFitToHeight(true);
        scrollCols.setVbarPolicy(ScrollBarPolicy.NEVER);
        scrollCols.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        TitledPane pane = new TitledPane("Configuracion de Notas", scrollCols);
        pane.setCollapsible(false);
        return pane;
    }

    private void bindIntegerField(TextField field, int fallbackValue, int minimumValue, IntConsumer setter) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            try {
                int parsedValue = Integer.parseInt(newValue.trim());
                if (parsedValue < minimumValue) {
                    throw new NumberFormatException("Value below minimum");
                }
                setter.accept(parsedValue);
                field.setStyle("");
                refreshPreview();
            } catch (NumberFormatException ex) {
                field.setStyle("-fx-border-color: #d9534f; -fx-border-width: 1.5;");
            }
        });

        try {
            setter.accept(Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ex) {
            setter.accept(fallbackValue);
        }
    }

    private void refreshPreview() {
        previewCanvas.drawPreview(maniaKeyConfig);
    }

    private void commitPercySpinnerValue(Spinner<Integer> spinner) {
        try {
            int value = Integer.parseInt(spinner.getEditor().getText().trim());
            maniaKeyConfig.setPercySize(value);
            spinner.getValueFactory().setValue(maniaKeyConfig.getPercySize());
            refreshPreview();
        } catch (NumberFormatException ex) {
            spinner.getValueFactory().setValue(maniaKeyConfig.getPercySize());
        }
    }

    private Color toFxColor(java.awt.Color color) {
        if (color == null) {
            return Color.WHITE;
        }
        return Color.rgb(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255.0);
    }

    private java.awt.Color toAwtColor(Color color) {
        if (color == null) {
            return java.awt.Color.WHITE;
        }

        return new java.awt.Color(
                toByte(color.getRed()),
                toByte(color.getGreen()),
                toByte(color.getBlue()),
                toByte(color.getOpacity())
        );
    }

    private int toByte(double component) {
        return Math.max(0, Math.min(255, (int) Math.round(component * 255.0)));
    }
}
