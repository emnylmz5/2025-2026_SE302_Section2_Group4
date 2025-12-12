package org.example.se302;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

public class ExamScheduleApp extends Application {

    private final MainController controller = new MainController();

    @Override
    public void start(Stage stage) {
        // Create buttons
        Button loadBtn = new Button("Load Data");
        Button genBtn = new Button("Generate Schedule");
        Button confBtn = new Button("Show Conflicts");

        // Wire buttons to controller methods
        loadBtn.setOnAction(e -> controller.onLoadData());
        genBtn.setOnAction(e -> controller.onGenerateSchedule());
        confBtn.setOnAction(e -> controller.onShowConflicts());

        // Simple toolbar at the top
        ToolBar toolBar = new ToolBar(loadBtn, genBtn, confBtn);

        // Placeholder center area for now
        Label centerLabel = new Label("Calendar view will be shown here.");

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(centerLabel);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        stage.setTitle("Exam Scheduler");
        stage.setScene(scene);
        stage.show();

        controller.initialize();
    }

    public static void main(String[] args) {
        launch();
    }
}