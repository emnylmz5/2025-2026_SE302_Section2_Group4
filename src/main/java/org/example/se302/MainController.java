package org.example.se302;

import java.util.function.Consumer;

public class MainController {

    private Calendar calendar;
    private SchedulingEngine schedulingEngine;
    private ConflictDetection conflictDetection;
    private CsvDataLoader dataLoader;

    // UI status sink (lets controller push messages to UI without depending on JavaFX classes)
    private Consumer<String> statusSink;

    public MainController() {
        this.schedulingEngine = new SchedulingEngine();
        this.conflictDetection = new ConflictDetection();
        this.dataLoader = new CsvDataLoader();
        this.statusSink = null;
    }

    public void initialize() {
        // Start with an empty calendar
        this.calendar = new Calendar();
        status("MainController initialized with an empty calendar.");
    }

    public void onLoadData() {
        // TODO: Ask the UI for file paths and use dataLoader to read CSV files.
        status("onLoadData() called - implement CSV loading using CsvDataLoader.");
    }

    /**
     * Overload used by the UI import window.
     * Required: studentsPath, coursesPath, classroomsPath, constraintsPath
     * Optional: attendanceListPath
     */
    public void onLoadData(String studentsPath,
                           String coursesPath,
                           String classroomsPath,
                           String attendanceListPath) {
        if (studentsPath == null || coursesPath == null || classroomsPath == null) {
            status("Load Data failed: missing required CSV path(s)." );
            return;
        }

        status("Loading CSVs..." );
        status("Students CSV: " + studentsPath);
        status("Courses CSV: " + coursesPath);
        status("Classrooms CSV: " + classroomsPath);
        status("Attendance CSV: " + (attendanceListPath == null ? "<none>" : attendanceListPath));

        // TODO: Uncomment once CsvDataLoader methods are implemented.
        // dataLoader.loadStudents(studentsPath);
        // dataLoader.loadCourses(coursesPath);
        // dataLoader.loadClassrooms(classroomsPath);
        // TODO: Load attendance list (optional) and wire enrollments.

        status("CSV paths received. Implement actual loading in CsvDataLoader.");
    }

    public void onGenerateSchedule() {
        // TODO: Retrieve loaded students, courses, classrooms, and constraints from the model.
        status("onGenerateSchedule() called - implement schedule generation using SchedulingEngine.");
    }

    public void onShowConflicts() {
        if (calendar == null) {
            status("No calendar generated yet. Cannot show conflicts.");
            return;
        }

        // TODO: Use ConflictDetection to compute conflicts and forward them to the view.
        status("onShowConflicts() called - implement conflict detection using ConflictDetection.");
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public SchedulingEngine getSchedulingEngine() {
        return schedulingEngine;
    }

    public void setSchedulingEngine(SchedulingEngine schedulingEngine) {
        this.schedulingEngine = schedulingEngine;
    }

    public ConflictDetection getConflictDetection() {
        return conflictDetection;
    }

    public void setConflictDetection(ConflictDetection conflictDetection) {
        this.conflictDetection = conflictDetection;
    }

    public CsvDataLoader getDataLoader() {
        return dataLoader;
    }

    public void setDataLoader(CsvDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    /**
     * Allows the UI layer to receive status messages.
     * Example usage from JavaFX: controller.setStatusSink(statusLabel::setText);
     */
    public void setStatusSink(Consumer<String> statusSink) {
        this.statusSink = statusSink;
    }

    /**
     * Sends a status message to stdout and (if set) to the UI sink.
     */
    private void status(String message) {
        System.out.println(message);
        if (statusSink != null) {
            statusSink.accept(message);
        }
    }
}
