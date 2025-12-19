package org.example.se302;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExamScheduleApp extends Application {

    private final MainController controller = new MainController();

    // UI state
    private TableView<ScheduleRow> calendarTable;
    private Label statusLabel;
    private Label calendarSummaryLabel;

    private Button importBtn;
    private Button constraintsBtn;
    private Button genBtn;
    private Button exportBtn;


    private Calendar lastCalendar;

    @Override
    public void start(Stage stage) {
        // Toolbar buttons
        importBtn = new Button("Import CSVs");
        constraintsBtn = new Button("Constraints");
        genBtn = new Button("Generate Schedule");
        genBtn.getStyleClass().add("primary-button");
        exportBtn = new Button("Export Calendar");

        // Status
        statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-label");

        // Controller -> UI (status)
        controller.setStatusSink(msg -> Platform.runLater(() -> statusLabel.setText(msg)));

        // Controller -> UI (calendar)
        controller.setCalendarSink(cal -> Platform.runLater(() -> {
            lastCalendar = cal;
            refreshScheduleTable(cal);
            updateButtonStates();
        }));


        // Actions
        importBtn.setOnAction(e -> openImportWindow(stage));
        constraintsBtn.setOnAction(e -> openConstraintsWindow(stage));
        genBtn.setOnAction(e -> {
            controller.onGenerateSchedule();

            // Fallback refresh: sink çalışmasa bile UI güncellensin
            Calendar cal = controller.getCalendar();
            if (cal != null) {
                lastCalendar = cal;
                refreshScheduleTable(cal);
                updateButtonStates();
            }
        });


        exportBtn.setOnAction(e -> exportCalendar(stage));

        // Center: Table + placeholder
        calendarTable = new TableView<>();
        calendarTable.getStyleClass().add("calendar-table");
        calendarTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        setupScheduleTableColumns(calendarTable);

        Label placeholder = new Label("No schedule yet. Import CSVs, then generate a schedule.");
        placeholder.getStyleClass().add("hint-label");
        placeholder.setWrapText(true);
        calendarTable.setPlaceholder(placeholder);

        calendarSummaryLabel = new Label("No schedule loaded.");
        calendarSummaryLabel.getStyleClass().add("calendar-summary");

        HBox calendarHeader = new HBox(calendarSummaryLabel);
        calendarHeader.getStyleClass().add("calendar-header");
        calendarHeader.setAlignment(Pos.CENTER_LEFT);

        // Center area: calendar scrollable

        // Toolbar layout
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        ToolBar toolBar = new ToolBar(leftSpacer, importBtn, constraintsBtn, exportBtn, genBtn, rightSpacer);

        BorderPane calendarPane = new BorderPane();
        calendarPane.setTop(calendarHeader);
        calendarPane.setCenter(calendarTable);
        calendarPane.getStyleClass().add("calendar-pane");
        BorderPane.setMargin(calendarTable, new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(calendarPane);
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        stage.setTitle("Exam Scheduler");
        stage.setScene(scene);
        stage.show();

        controller.initialize();
        updateButtonStates();
    }

    private void setupScheduleTableColumns(TableView<ScheduleRow> table) {
        TableColumn<ScheduleRow, String> colCourse = new TableColumn<>("Course");
        colCourse.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().courseCode()));

        TableColumn<ScheduleRow, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().date()));

        TableColumn<ScheduleRow, String> colStart = new TableColumn<>("Start");
        colStart.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().startTime()));

        TableColumn<ScheduleRow, String> colEnd = new TableColumn<>("End");
        colEnd.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().endTime()));

        TableColumn<ScheduleRow, String> colDur = new TableColumn<>("Duration");
        colDur.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().duration()));

        TableColumn<ScheduleRow, String> colRooms = new TableColumn<>("Rooms");
        colRooms.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().rooms()));

        TableColumn<ScheduleRow, String> colCount = new TableColumn<>("Students");
        colCount.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().studentCount()));

        table.getColumns().setAll(colCourse, colDate, colStart, colEnd, colDur, colRooms, colCount);
    }

    private void refreshScheduleTable(Calendar calendar) {
        if (calendar == null || calendar.getExamSessions() == null) {
            calendarTable.setItems(FXCollections.observableArrayList());
            if (calendarSummaryLabel != null) {
                calendarSummaryLabel.setText("No schedule loaded.");
            }
            return;
        }

        ObservableList<ScheduleRow> rows = FXCollections.observableArrayList();

        for (ExamSession s : calendar.getExamSessions()) {
            if (s == null) continue;

            String courseCode = safe(s.getCourseCode());

            LocalDateTime st = s.getStartDateTime();
            Integer durMin = s.getDurationMinutes();

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

            String date = (st == null) ? "" : st.format(df);
            String startTime = (st == null) ? "" : st.format(tf);

            String endTime = "";
            if (st != null && durMin != null) {
                try {
                    endTime = st.plusMinutes(durMin).format(tf);
                } catch (Exception ignored) {
                    endTime = "";
                }
            }

            String duration = (durMin == null) ? "" : (durMin + " min");

            List<String> roomIds = new ArrayList<>();
            int total = 0;

            if (s.getRoomAssignments() != null) {
                for (ExamRoomAssignment a : s.getRoomAssignments()) {
                    if (a == null) continue;
                    if (a.getRoom() != null) roomIds.add(safe(a.getRoom().getClassroomId()));
                    total += a.getStudentCount();
                }
            }

            rows.add(new ScheduleRow(
                    courseCode,
                    date,
                    startTime,
                    endTime,
                    duration,
                    String.join(", ", roomIds),
                    String.valueOf(total)
            ));
        }

        calendarTable.setItems(rows);
        if (calendarSummaryLabel != null) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            calendarSummaryLabel.setText("Sessions: " + rows.size() + "  |  Updated: " + ts);
        }
    }

    private void updateButtonStates() {
        boolean hasData = controller.hasLoadedData();
        boolean hasSchedule = lastCalendar != null
                && lastCalendar.getExamSessions() != null
                && !lastCalendar.getExamSessions().isEmpty();

        genBtn.setDisable(!hasData);
        constraintsBtn.setDisable(!hasData);

        exportBtn.setDisable(!hasSchedule);
    }

    private void openImportWindow(Stage owner) {
        final String[] studentsPath = new String[1];
        final String[] coursesPath = new String[1];
        final String[] classroomsPath = new String[1];
        final String[] attendancePath = new String[1];

        Stage importStage = new Stage();
        importStage.initOwner(owner);
        importStage.initModality(Modality.WINDOW_MODAL);
        importStage.setTitle("CSV Import");

        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv");
        fc.getExtensionFilters().setAll(csvFilter);
        fc.setSelectedExtensionFilter(csvFilter);

        Label hint = new Label(
                "Select each CSV file via buttons, then click 'Load Selected'.\n" +
                        "Required: Students, Courses, Classrooms, Attendance List."
        );
        hint.setWrapText(true);
        hint.setMaxWidth(680);
        hint.getStyleClass().add("hint-label");

        Label importStatus = new Label("Tip: Select all required CSVs first.");
        importStatus.getStyleClass().add("hint-label");

        Label studentsLabel = new Label("Students: Not Selected");
        Label coursesLabel = new Label("Courses: Not Selected");
        Label classroomsLabel = new Label("Classrooms: Not Selected");
        Label attendanceLabel = new Label("Attendance List: Not Selected");

        Button pickStudents = new Button("Select Students CSV");
        Button pickCourses = new Button("Select Courses CSV");
        Button pickClassrooms = new Button("Select Classrooms CSV");
        Button pickAttendance = new Button("Select Attendance CSV");

        Button loadSelected = new Button("Load Selected");
        loadSelected.getStyleClass().add("primary-button");
        loadSelected.setDefaultButton(true);
        loadSelected.setDisable(true);

        Runnable updateLoadEnabled = () -> {
            boolean ready = studentsPath[0] != null
                    && coursesPath[0] != null
                    && classroomsPath[0] != null
                    && attendancePath[0] != null;
            loadSelected.setDisable(!ready);
        };

        pickStudents.setOnAction(e -> {
            fc.setTitle("Select Students CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                studentsPath[0] = f.getAbsolutePath();
                studentsLabel.setText("Students: " + f.getName());
                updateLoadEnabled.run();
            }
        });

        pickCourses.setOnAction(e -> {
            fc.setTitle("Select Courses CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                coursesPath[0] = f.getAbsolutePath();
                coursesLabel.setText("Courses: " + f.getName());
                updateLoadEnabled.run();
            }
        });

        pickClassrooms.setOnAction(e -> {
            fc.setTitle("Select Classrooms CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                classroomsPath[0] = f.getAbsolutePath();
                classroomsLabel.setText("Classrooms: " + f.getName());
                updateLoadEnabled.run();
            }
        });

        pickAttendance.setOnAction(e -> {
            fc.setTitle("Select Attendance List CSV");
            File f = fc.showOpenDialog(importStage);
            if (f != null) {
                attendancePath[0] = f.getAbsolutePath();
                attendanceLabel.setText("Attendance List: " + f.getName());
                updateLoadEnabled.run();
            }
        });

        loadSelected.setOnAction(e -> {
            if (studentsPath[0] == null || coursesPath[0] == null || classroomsPath[0] == null || attendancePath[0] == null) {
                importStatus.setText("Missing required CSV(s): Students, Courses, Classrooms, Attendance List.");
                return;
            }

            controller.onLoadData(studentsPath[0], coursesPath[0], classroomsPath[0], attendancePath[0]);

            // reset visual state
            lastCalendar = null;
            refreshScheduleTable(null);
            updateButtonStates();

            statusLabel.setText("Data loaded.");
            importStage.close();
        });

        VBox selectors = new VBox(14);
        selectors.setAlignment(Pos.CENTER);
        selectors.setMaxWidth(680);
        selectors.setFillWidth(true);

        for (Button b : List.of(pickStudents, pickCourses, pickClassrooms, pickAttendance)) {
            b.setMinWidth(280);
            b.setPrefWidth(280);
        }
        for (Label l : List.of(studentsLabel, coursesLabel, classroomsLabel, attendanceLabel)) {
            l.setWrapText(true);
            l.setPrefWidth(280);
            l.setMaxWidth(280);
            l.setAlignment(Pos.CENTER);
        }

        selectors.getChildren().addAll(
                new VBox(6, pickStudents, studentsLabel),
                new VBox(6, pickCourses, coursesLabel),
                new VBox(6, pickClassrooms, classroomsLabel),
                new VBox(6, pickAttendance, attendanceLabel)
        );
        selectors.getChildren().forEach(n -> ((VBox) n).setAlignment(Pos.CENTER));

        importStatus.setWrapText(true);
        importStatus.setPrefWidth(680);
        importStatus.setMaxWidth(680);

        VBox primaryAction = new VBox(6, actions(loadSelected), rightAligned(importStatus));
        primaryAction.setAlignment(Pos.CENTER);
        primaryAction.setFillWidth(true);
        primaryAction.setMaxWidth(680);

        VBox content = new VBox(12, hint, selectors, primaryAction);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("import-root");
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);

        Scene importScene = new Scene(content, 720, 390);
        importScene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        importStage.setScene(importScene);
        importStage.setResizable(false);
        importStage.show();
    }

    private void openConstraintsWindow(Stage owner) {
        Constraints current = controller.getConstraints();
        if (current == null) current = new Constraints();

        Stage w = new Stage();
        w.initOwner(owner);
        w.initModality(Modality.WINDOW_MODAL);
        w.setTitle("Constraints Configuration");

        Spinner<Integer> minGap = new Spinner<>(0, 600, current.getMinMinutesBetweenExams(), 15);
        Spinner<Integer> maxPerDay = new Spinner<>(1, 10, current.getMaxExamsPerDay(), 1);
        minGap.setEditable(true);
        maxPerDay.setEditable(true);

        // Exam week dates
        DatePicker examStartPicker = new DatePicker();
        DatePicker examEndPicker = new DatePicker();

        // DatePicker format: dd.MM.yyyy (TR)
        DateTimeFormatter trDateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        StringConverter<LocalDate> trDateConverter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return (date == null) ? "" : trDateFmt.format(date);
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null) return null;
                String s = string.trim();
                if (s.isEmpty()) return null;
                return LocalDate.parse(s, trDateFmt);
            }
        };
        examStartPicker.setConverter(trDateConverter);
        examEndPicker.setConverter(trDateConverter);
        examStartPicker.setPromptText("gg.aa.yyyy");
        examEndPicker.setPromptText("gg.aa.yyyy");

        LocalDate curStart = current.getExamWeekStartDate();
        LocalDate curEnd = current.getExamWeekEndDate();

        LocalDate today = LocalDate.now();
        examStartPicker.setValue(curStart != null ? curStart : today);
        examEndPicker.setValue(curEnd != null ? curEnd : today.plusWeeks(1));

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHalignment(javafx.geometry.HPos.LEFT);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setHalignment(javafx.geometry.HPos.LEFT);
        form.getColumnConstraints().addAll(c0, c1, c2, c3);

        // row 0: spacing rules
        form.add(new Label("Min break between exams (students, min)"), 0, 0);
        form.add(minGap, 1, 0);
        form.add(new Label("Max exams per student/day"), 2, 0);
        form.add(maxPerDay, 3, 0);

        // Allowed days (UI)
        List<CheckBox> dayChecks = new ArrayList<>();
        HBox daysBox = new HBox(14);
        daysBox.setAlignment(Pos.CENTER_LEFT);

        for (DayOfWeek d : DayOfWeek.values()) {
            CheckBox cb = new CheckBox(d.name().substring(0, 3));
            cb.setSelected(current.getAllowedDays() != null && current.getAllowedDays().contains(d));
            dayChecks.add(cb);
            daysBox.getChildren().add(cb);
        }

        // row 1: allowed days
        form.add(new Label("Allowed days"), 0, 1);
        form.add(daysBox, 1, 1, 3, 1);

        ComboBox<String> startTime = new ComboBox<>();
        ComboBox<String> endTime = new ComboBox<>();
        startTime.getItems().addAll("08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00");
        endTime.getItems().addAll("12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00");
        startTime.setValue("09:00");
        endTime.setValue("17:00");

        HBox timeBox = new HBox(8, startTime, new Label("to"), endTime);
        timeBox.setAlignment(Pos.CENTER);

        // row 2: allowed time range
        form.add(new Label("Exam slot time range"), 0, 2);
        form.add(timeBox, 1, 2, 3, 1);

        // row 3: exam week dates (side-by-side)
        form.add(new Label("Exam week start"), 0, 3);
        form.add(examStartPicker, 1, 3);
        form.add(new Label("Exam week end"), 2, 3);
        form.add(examEndPicker, 3, 3);

        // --- Exam duration calculation coefficients ---
        Separator coefSep = new Separator();
        coefSep.setMaxWidth(Double.MAX_VALUE);
        coefSep.getStyleClass().add("constraints-section-separator");
        form.add(coefSep, 0, 4, 4, 1);

        Label coefTitle = new Label("Exam duration coefficients");
        coefTitle.getStyleClass().add("constraints-section-title");
        coefTitle.setMaxWidth(Double.MAX_VALUE);
        coefTitle.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(coefTitle, Priority.ALWAYS);
        form.add(coefTitle, 0, 5, 4, 1);

        Label coefHint = new Label("Adjust how exam duration is calculated.");
        coefHint.getStyleClass().add("constraints-section-hint");
        coefHint.setWrapText(true);
        coefHint.setMaxWidth(Double.MAX_VALUE);
        coefHint.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(coefHint, Priority.ALWAYS);
        form.add(coefHint, 0, 6, 4, 1);

        Spinner<Integer> baseMinutes = new Spinner<>(0, 600, current.getBaseExamDurationMinutes(), 5);
        baseMinutes.setEditable(true);

        Spinner<Integer> minutesPerCredit = new Spinner<>(0, 300, current.getCreditDurationCoefficientMinutes(), 1);
        minutesPerCredit.setEditable(true);

        Spinner<Integer> roundingMinutes = new Spinner<>(1, 60, current.getDurationRoundingMinutes(), 1);
        roundingMinutes.setEditable(true);

        Spinner<Integer> minDuration = new Spinner<>(1, 600, current.getMinExamDurationMinutes(), 5);
        minDuration.setEditable(true);

        // row 7: base + per-credit side-by-side
        form.add(new Label("Base duration (min)"), 0, 7);
        form.add(baseMinutes, 1, 7);
        form.add(new Label("Minutes per credit"), 2, 7);
        form.add(minutesPerCredit, 3, 7);

        // row 8: rounding + minimum duration
        form.add(new Label("Round to (min)"), 0, 8);
        form.add(roundingMinutes, 1, 8);
        form.add(new Label("Minimum exam duration (min)"), 2, 8);
        form.add(minDuration, 3, 8);

        Label note = new Label("These constraints will be used the next time you generate a schedule.");
        note.getStyleClass().add("hint-label");
        note.setWrapText(true);
        note.setMaxWidth(Double.MAX_VALUE);
        note.setAlignment(Pos.CENTER);

        Button save = new Button("Save Constraints");
        save.getStyleClass().add("primary-button");
        save.setDefaultButton(true);

        save.setOnAction(e -> {
            try {
                Constraints c = new Constraints();
                c.setMinMinutesBetweenExams(minGap.getValue());
                c.setMaxExamsPerDay(maxPerDay.getValue());

                // days
                List<DayOfWeek> allowed = new ArrayList<>();
                DayOfWeek[] all = DayOfWeek.values();
                for (int i = 0; i < all.length; i++) {
                    if (dayChecks.get(i).isSelected()) allowed.add(all[i]);
                }
                c.setAllowedDays(allowed);

                // time range
                LocalTime st = LocalTime.parse(startTime.getValue());
                LocalTime en = LocalTime.parse(endTime.getValue());
                c.setAllowedTimeRanges(List.of(new Constraints.TimeRange(st, en)));

                // exam week dates (optional)
                c.setExamWeekStartDate(examStartPicker.getValue());
                c.setExamWeekEndDate(examEndPicker.getValue());

                // exam duration coefficients
                c.setBaseExamDurationMinutes(baseMinutes.getValue());
                c.setCreditDurationCoefficientMinutes(minutesPerCredit.getValue());
                c.setDurationRoundingMinutes(roundingMinutes.getValue());
                c.setMinExamDurationMinutes(minDuration.getValue());

                controller.setConstraints(c);
                statusLabel.setText("Constraints saved.");
                w.close();
            } catch (Exception ex) {
                statusLabel.setText("Invalid constraints: " + ex.getMessage());
            }
        });

        Region spacer = new Region();
        VBox root = new VBox(12, form, note, spacer, actions(save));
        root.setPadding(new Insets(14));
        root.setAlignment(Pos.TOP_CENTER);
        root.setFillWidth(true);
        root.getStyleClass().add("import-root");
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Scene scene = new Scene(root, 860, 500);
        scene.getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        w.setScene(scene);
        w.setResizable(false);
        w.show();
    }

    private void exportCalendar(Stage owner) {
        if (lastCalendar == null || lastCalendar.getExamSessions() == null || lastCalendar.getExamSessions().isEmpty()) {
            statusLabel.setText("No schedule to export.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Exam Calendar (CSV)");
        fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        fc.setInitialFileName("exam_schedule.csv");

        File out = fc.showSaveDialog(owner);
        if (out == null) return;

        String path = out.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".csv")) path += ".csv";

        try {
            CsvDataWriter writer = new CsvDataWriter();
            writer.writeExamSchedule(lastCalendar, path);
            statusLabel.setText("Exported: " + path);
        } catch (Exception ex) {
            statusLabel.setText("Export failed: " + ex.getMessage());
        }
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

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private record ScheduleRow(
            String courseCode,
            String date,
            String startTime,
            String endTime,
            String duration,
            String rooms,
            String studentCount
    ) {}

    public static void main(String[] args) {
        launch();
    }
}