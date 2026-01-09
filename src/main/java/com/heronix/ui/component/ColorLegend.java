package com.heronix.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Color Legend Component
 * Location: src/main/java/com/eduscheduler/ui/component/ColorLegend.java
 * 
 * Displays a legend showing subject color coding
 */
public class ColorLegend extends VBox {

    public ColorLegend() {
        setupLegend();
    }

    private void setupLegend() {
        setPadding(new Insets(10));
        setSpacing(8);
        setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1px;");

        Label title = new Label("Subject Colors");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        getChildren().add(title);

        // Get color schemes from ModernCalendarGrid
        Map<String, String> mainColors = new LinkedHashMap<>();
        mainColors.put("Mathematics", ModernCalendarGrid.ColorScheme.MATHEMATICS.getColorHex());
        mainColors.put("Science", ModernCalendarGrid.ColorScheme.SCIENCE.getColorHex());
        mainColors.put("English", ModernCalendarGrid.ColorScheme.ENGLISH.getColorHex());
        mainColors.put("History", ModernCalendarGrid.ColorScheme.HISTORY.getColorHex());
        mainColors.put("Physical Education", ModernCalendarGrid.ColorScheme.PHYSICAL_EDUCATION.getColorHex());
        mainColors.put("Arts", ModernCalendarGrid.ColorScheme.ARTS.getColorHex());
        mainColors.put("Technology", ModernCalendarGrid.ColorScheme.TECHNOLOGY.getColorHex());
        mainColors.put("Business", ModernCalendarGrid.ColorScheme.BUSINESS.getColorHex());
        mainColors.put("Lunch", ModernCalendarGrid.ColorScheme.LUNCH.getColorHex());
        mainColors.put("Other", ModernCalendarGrid.ColorScheme.OTHER.getColorHex());

        for (Map.Entry<String, String> entry : mainColors.entrySet()) {
            getChildren().add(createLegendItem(entry.getKey(), entry.getValue()));
        }
    }

    private HBox createLegendItem(String label, String colorHex) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);

        Rectangle colorBox = new Rectangle(16, 16);
        colorBox.setFill(Color.web(colorHex));
        colorBox.setStroke(Color.web("#cbd5e1"));

        Label labelText = new Label(label);
        labelText.setFont(Font.font("Segoe UI", 10));

        item.getChildren().addAll(colorBox, labelText);
        return item;
    }

    public static ColorLegend create() {
        return new ColorLegend();
    }
}