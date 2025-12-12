package org.example.se302;

import java.util.List;

public class MainController {

    private Calendar calendar;
    private SchedulingEngine schedulingEngine;
    private ConflictDetection conflictDetection;
    private CsvDataLoader dataLoader;

    public MainController() {
        this.schedulingEngine = new SchedulingEngine();
        this.conflictDetection = new ConflictDetection();
        this.dataLoader = new CsvDataLoader();
    }

    public void initialize() {
        // Start with an empty calendar
        this.calendar = new Calendar();
        System.out.println("MainController initialized with an empty calendar.");
    }

    public void onLoadData() {
        // TODO: Ask the UI for file paths and use dataLoader to read CSV files.
        System.out.println("onLoadData() called - implement CSV loading using CsvDataLoader.");
    }

    public void onGenerateSchedule() {
        // TODO: Retrieve loaded students, courses, classrooms, and constraints from the model.
        System.out.println("onGenerateSchedule() called - implement schedule generation using SchedulingEngine.");
    }

    public void onShowConflicts() {
        if (calendar == null) {
            System.out.println("No calendar generated yet. Cannot show conflicts.");
            return;
        }

        // TODO: Use ConflictDetection to compute conflicts and forward them to the view.
        System.out.println("onShowConflicts() called - implement conflict detection using ConflictDetection.");
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
}
