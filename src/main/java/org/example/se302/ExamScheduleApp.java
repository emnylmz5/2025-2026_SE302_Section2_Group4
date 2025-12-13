package org.example.se302;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;

public class ExamScheduleApp extends Application {

    private final MainController controller = new MainController();

    @Override
    public void start(Stage stage) {
        // Main toolbar buttons
        Button importBtn = new Button("Import CSVs");
        Button genBtn = new Button("Generate Schedule");
        Button exportBtn = new Button("Export Calendar");
        Button confBtn = new Button("Show Conflicts");

        // Status area (bottom)
        Label statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-label");

        // Let controller push status messages into the UI (if controller supports it)
        controller.setStatusSink(statusLabel::setText);

        // Wire actions
        importBtn.setOnAction(e -> openImportWindow(stage));
        genBtn.setOnAction(e -> controller.onGenerateSchedule());
        confBtn.setOnAction(e -> controller.onShowConflicts());
        exportBtn.setOnAction(e -> statusLabel.setText("Export Calendar clicked - implement export via CsvDataWriter."));

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(leftSpacer, importBtn, genBtn, exportBtn, confBtn, rightSpacer);

        // Center area: placeholder table for calendar sessions (later bind to CalendarViewController)
        TableView<String> calendarTable = new TableView<>();
        calendarTable.setPlaceholder(new Label("No schedule yet. Import CSVs, then generate a schedule."));

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(calendarTable);
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        stage.setTitle("Exam Scheduler");
        stage.setScene(scene);
        stage.show();

        controller.initialize();
    }

    private void openImportWindow(Stage owner) {
        // Keep selected paths in memory until the user clicks "Load Selected"
        final String[] studentsPath = new String[1];
        final String[] coursesPath = new String[1];
        final String[] classroomsPath = new String[1];
        final String[] attendancePath = new String[1];

        Stage importStage = new Stage();
        importStage.initOwner(owner);
        importStage.initModality(Modality.WINDOW_MODAL);
        importStage.setTitle("CSV Import");

        // Open this window near the right edge of the screen
        importStage.setOnShown(ev -> {
            var bounds = Screen.getPrimary().getVisualBounds();
            importStage.setX(bounds.getMaxX() - importStage.getWidth() - 20);
            importStage.setY(bounds.getMinY() + 60);
        });

        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv");
        fc.getExtensionFilters().setAll(csvFilter);
        fc.setSelectedExtensionFilter(csvFilter);

        Label hint = new Label(
                "Select each CSV with its own button, then click 'Load Selected'.\n" +
                "Required: Students, Courses, Classrooms. Optional: Attendance List."
        );
        hint.setWrapText(true);
        hint.setMaxWidth(680);
        hint.getStyleClass().add("hint-label");

        Label importStatus = new Label("Tip: Select required CSVs first.");
        importStatus.getStyleClass().add("hint-label");

        // Labels show the chosen file names
        Label studentsLabel = new Label("Students: not selected");
        Label coursesLabel = new Label("Courses: not selected");
        Label classroomsLabel = new Label("Classrooms: not selected");
        Label attendanceLabel = new Label("Attendance List (optional): not selected");

        // One button per CSV
        Button pickStudents = new Button("Select Students CSV");
        Button pickCourses = new Button("Select Courses CSV");
        Button pickClassrooms = new Button("Select Classrooms CSV");
        Button pickAttendance = new Button("Select Attendance CSV");

        pickStudents.setOnAction(e -> {
            fc.setTitle("Select Students CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                if (!f.getName().toLowerCase().endsWith(".csv")) {
                    importStatus.setText("Only .csv files are allowed.");
                    return;
                }
                studentsPath[0] = f.getAbsolutePath();
                studentsLabel.setText("Students: " + f.getName());
            }
        });

        pickCourses.setOnAction(e -> {
            fc.setTitle("Select Courses CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                if (!f.getName().toLowerCase().endsWith(".csv")) {
                    importStatus.setText("Only .csv files are allowed.");
                    return;
                }
                coursesPath[0] = f.getAbsolutePath();
                coursesLabel.setText("Courses: " + f.getName());
            }
        });

        pickClassrooms.setOnAction(e -> {
            fc.setTitle("Select Classrooms CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                if (!f.getName().toLowerCase().endsWith(".csv")) {
                    importStatus.setText("Only .csv files are allowed.");
                    return;
                }
                classroomsPath[0] = f.getAbsolutePath();
                classroomsLabel.setText("Classrooms: " + f.getName());
            }
        });

        pickAttendance.setOnAction(e -> {
            fc.setTitle("Select Attendance List CSV (optional)");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                if (!f.getName().toLowerCase().endsWith(".csv")) {
                    importStatus.setText("Only .csv files are allowed.");
                    return;
                }
                attendancePath[0] = f.getAbsolutePath();
                attendanceLabel.setText("Attendance List (optional): " + f.getName());
            }
        });

        Button loadSelected = new Button("Load Selected");
        loadSelected.getStyleClass().add("primary-button");
        loadSelected.setDefaultButton(true);

        loadSelected.setOnAction(e -> {
            if (studentsPath[0] == null || coursesPath[0] == null || classroomsPath[0] == null) {
                importStatus.setText("Missing required CSV(s): Students, Courses, Classrooms.");
                return;
            }

            controller.onLoadData(
                    studentsPath[0],
                    coursesPath[0],
                    classroomsPath[0],
                    attendancePath[0]
            );

            importStatus.setText("Loaded! Close this window (X) and click 'Generate Schedule'.");
        });

        // Centered selectors: each button centered; its label ONLY under it; labels right-aligned
        VBox selectors = new VBox(14);
        selectors.setAlignment(Pos.CENTER);
        selectors.setMaxWidth(680);
        selectors.setFillWidth(true);

        // Ensure consistent button width
        pickStudents.setMinWidth(280);
        pickStudents.setPrefWidth(280);
        pickCourses.setMinWidth(280);
        pickCourses.setPrefWidth(280);
        pickClassrooms.setMinWidth(280);
        pickClassrooms.setPrefWidth(280);
        pickAttendance.setMinWidth(280);
        pickAttendance.setPrefWidth(280);

        // File name labels: centered and ONLY under their buttons
        studentsLabel.setWrapText(true);
        coursesLabel.setWrapText(true);
        classroomsLabel.setWrapText(true);
        attendanceLabel.setWrapText(true);

        studentsLabel.setPrefWidth(280);
        coursesLabel.setPrefWidth(280);
        classroomsLabel.setPrefWidth(280);
        attendanceLabel.setPrefWidth(280);

        studentsLabel.setMaxWidth(280);
        coursesLabel.setMaxWidth(280);
        classroomsLabel.setMaxWidth(280);
        attendanceLabel.setMaxWidth(280);

        studentsLabel.setAlignment(Pos.CENTER);
        coursesLabel.setAlignment(Pos.CENTER);
        classroomsLabel.setAlignment(Pos.CENTER);
        attendanceLabel.setAlignment(Pos.CENTER);

        // Removed redundant setAlignment calls for labels

        VBox studentsBox = new VBox(6, pickStudents, studentsLabel);
        studentsBox.setAlignment(Pos.CENTER);
        studentsBox.setFillWidth(true);
        studentsBox.setMaxWidth(680);

        VBox coursesBox = new VBox(6, pickCourses, coursesLabel);
        coursesBox.setAlignment(Pos.CENTER);
        coursesBox.setFillWidth(true);
        coursesBox.setMaxWidth(680);

        VBox classroomsBox = new VBox(6, pickClassrooms, classroomsLabel);
        classroomsBox.setAlignment(Pos.CENTER);
        classroomsBox.setFillWidth(true);
        classroomsBox.setMaxWidth(680);

        VBox attendanceBox = new VBox(6, pickAttendance, attendanceLabel);
        attendanceBox.setAlignment(Pos.CENTER);
        attendanceBox.setFillWidth(true);
        attendanceBox.setMaxWidth(680);

        selectors.getChildren().addAll(studentsBox, coursesBox, classroomsBox, attendanceBox);

        // Load Selected + status (status under the button, aligned to the far right)
        importStatus.setWrapText(true);
        importStatus.setPrefWidth(680);
        importStatus.setMaxWidth(680);

        // Removed redundant setAlignment call for importStatus

        VBox primaryAction = new VBox(6, actions(loadSelected), rightAligned(importStatus));
        primaryAction.setAlignment(Pos.CENTER);
        primaryAction.setFillWidth(true);
        primaryAction.setMaxWidth(680);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("import-root");
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        content.getChildren().addAll(
                hint,
                selectors,
                primaryAction
        );

        Scene importScene = new Scene(content, 720, 390);
        importScene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        importStage.setScene(importScene);
        importStage.show();
    }

    private HBox row(Button button, Label label) {
        button.setMinWidth(220);
        button.setPrefWidth(220);

        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(12, button, label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(680);

        HBox.setHgrow(label, Priority.ALWAYS);
        return row;
    }

    private HBox actions(Button primary, Button secondary) {
        HBox box = new HBox(10, primary, secondary);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("import-actions");
        return box;
    }

    private HBox actions(Button primary) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(spacer, primary);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setMaxWidth(680);
        box.getStyleClass().add("import-actions");
        return box;
    }

    private HBox rightAligned(Label label) {
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);

        HBox box = new HBox(label);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setMaxWidth(680);
        HBox.setHgrow(label, Priority.ALWAYS);
        return box;
    }

    public static void main(String[] args) {
        launch();
    }
}