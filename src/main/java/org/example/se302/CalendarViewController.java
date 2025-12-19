package org.example.se302;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CalendarViewController {

    private final VBox root;
    private final DatePicker datePicker;
    private final Label placeholder;
    private final TableView<ExamSession> table;
    private final ObservableList<ExamSession> tableItems;

    private Calendar calendar;
    private Consumer<String> statusSink;
    private Consumer<ExamSession> onSessionSelected;

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public CalendarViewController() {
        this.root = new VBox(12);
        this.root.setPadding(new Insets(14));
        this.root.setAlignment(Pos.TOP_CENTER);

        this.datePicker = new DatePicker(LocalDate.now());
        this.datePicker.setMaxWidth(220);

        this.placeholder = new Label("No exam sessions for the selected date.");
        this.placeholder.getStyleClass().add("calendar-placeholder");
        this.placeholder.setMaxWidth(Double.MAX_VALUE);
        this.placeholder.setAlignment(Pos.CENTER);

        this.table = new TableView<>();
        this.table.getStyleClass().add("calendar-table");
        this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        this.tableItems = FXCollections.observableArrayList();
        this.table.setItems(tableItems);

        initColumns();
        wireActions();

        VBox.setVgrow(table, Priority.ALWAYS);
        this.root.getChildren().addAll(datePicker, placeholder, table);

        updateVisibility();
    }

    public Parent getView() {
        return root;
    }

    public void setStatusSink(Consumer<String> statusSink) {
        this.statusSink = statusSink;
    }

    public void setOnSessionSelected(Consumer<ExamSession> onSessionSelected) {
        this.onSessionSelected = onSessionSelected;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
        refresh();
    }

    public void refresh() {
        tableItems.clear();

        if (calendar == null) {
            status("Calendar is not set.");
            updateVisibility();
            return;
        }

        LocalDate d = datePicker.getValue();
        List<ExamSession> sessions = (d == null) ? calendar.getExamSessions() : calendar.getSessionsByDate(d);
        tableItems.addAll(sessions);

        status("Calendar view updated: sessions=" + sessions.size());
        updateVisibility();
    }

    private void initColumns() {
        TableColumn<ExamSession, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(courseCodeOf(cd.getValue())));

        TableColumn<ExamSession, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(dateOf(cd.getValue())));

        TableColumn<ExamSession, String> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(startTimeOf(cd.getValue())));

        TableColumn<ExamSession, String> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(endTimeOf(cd.getValue())));

        TableColumn<ExamSession, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(durationOf(cd.getValue())));

        TableColumn<ExamSession, String> roomsCol = new TableColumn<>("Rooms");
        roomsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(roomsOf(cd.getValue())));

        TableColumn<ExamSession, String> studentsCol = new TableColumn<>("Students");
        studentsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(studentsCountOf(cd.getValue())));

        table.getColumns().setAll(courseCol, dateCol, startCol, endCol, durationCol, roomsCol, studentsCol);
    }

    private void wireActions() {
        datePicker.valueProperty().addListener((obs, oldV, newV) -> refresh());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && onSessionSelected != null) {
                onSessionSelected.accept(newV);
            }
        });
    }

    private void updateVisibility() {
        boolean hasData = !tableItems.isEmpty();
        table.setVisible(hasData);
        table.setManaged(hasData);

        placeholder.setVisible(!hasData);
        placeholder.setManaged(!hasData);
    }

    private void status(String msg) {
        if (statusSink != null && msg != null) {
            statusSink.accept(msg);
        }
    }

    private String courseCodeOf(ExamSession s) {
        if (s == null) return "";

        Object courseObj = tryInvoke(s, "getCourse");
        if (courseObj == null) courseObj = tryGetField(s, "course");

        if (courseObj != null) {
            Object code = tryInvoke(courseObj, "getCourseCode");
            if (code == null) code = tryGetField(courseObj, "courseCode");
            if (code != null) return String.valueOf(code);
        }

        Object codeDirect = tryInvoke(s, "getCourseCode");
        if (codeDirect == null) codeDirect = tryGetField(s, "courseCode");
        return codeDirect == null ? "" : String.valueOf(codeDirect);
    }

    private String dateOf(ExamSession s) {
        LocalDateTime dt = startDateTimeOf(s);
        return dt == null ? "" : dateFmt.format(dt);
    }

    private String startTimeOf(ExamSession s) {
        LocalDateTime dt = startDateTimeOf(s);
        return dt == null ? "" : timeFmt.format(dt);
    }

    private String endTimeOf(ExamSession s) {
        LocalDateTime dt = startDateTimeOf(s);
        Integer dur = durationMinutesOf(s);
        if (dt == null || dur == null) return "";
        try {
            return timeFmt.format(dt.plusMinutes(dur));
        } catch (Exception ignored) {
            return "";
        }
    }

    private LocalDateTime startDateTimeOf(ExamSession s) {
        Object v = tryInvoke(s, "getStartDateTime");
        if (v == null) v = tryGetField(s, "startDateTime");
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        return null;
    }
    private Integer durationMinutesOf(ExamSession s) {
        if (s == null) return null;

        Object v = tryInvoke(s, "getDurationMinutes");
        if (v == null) v = tryGetField(s, "durationMinutes");
        if (v == null) v = tryInvoke(s, "getDuration");
        if (v == null) v = tryGetField(s, "duration");
        if (v == null) return null;

        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignored) {
            return null;
        }
    }


    private String durationOf(ExamSession s) {
        Object v = tryInvoke(s, "getDurationMinutes");
        if (v == null) v = tryGetField(s, "durationMinutes");
        if (v == null) v = tryInvoke(s, "getDuration");
        if (v == null) v = tryGetField(s, "duration");

        if (v == null) return "";
        try {
            int mins = Integer.parseInt(String.valueOf(v));
            return mins + " min";
        } catch (Exception ignored) {
        }
        return String.valueOf(v);
    }

    private String roomsOf(ExamSession s) {
        Object assignments = tryInvoke(s, "getRoomAssignments");
        if (assignments == null) assignments = tryGetField(s, "roomAssignments");
        if (!(assignments instanceof Iterable<?>)) return "";

        List<String> rooms = new ArrayList<>();
        for (Object a : (Iterable<?>) assignments) {
            if (a == null) continue;

            Object roomObj = tryInvoke(a, "getRoom");
            if (roomObj == null) roomObj = tryGetField(a, "room");

            if (roomObj != null) {
                Object id = tryInvoke(roomObj, "getClassroomId");
                if (id == null) id = tryInvoke(roomObj, "getId");
                if (id == null) id = tryGetField(roomObj, "classroomId");
                rooms.add(id == null ? String.valueOf(roomObj) : String.valueOf(id));
            }
        }

        return String.join(", ", rooms);
    }

    private String studentsCountOf(ExamSession s) {
        Object v = tryInvoke(s, "getAllStudents");
        if (v == null) v = tryGetField(s, "allStudents");

        if (v instanceof Iterable<?>) {
            int count = 0;
            for (Object ignored : (Iterable<?>) v) count++;
            return String.valueOf(count);
        }

        Object assignments = tryInvoke(s, "getRoomAssignments");
        if (assignments == null) assignments = tryGetField(s, "roomAssignments");
        if (assignments instanceof Iterable<?>) {
            int count = 0;
            for (Object a : (Iterable<?>) assignments) {
                Object list = tryInvoke(a, "getStudents");
                if (list == null) list = tryGetField(a, "students");
                if (list instanceof Iterable<?>) {
                    for (Object ignored : (Iterable<?>) list) count++;
                }
            }
            return String.valueOf(count);
        }

        return "";
    }

    private Object tryInvoke(Object target, String methodName) {
        if (target == null || methodName == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object tryGetField(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception ignored) {
            return null;
        }
    }
}