package org.example.se302;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MainController {

    private Calendar calendar;
    private SchedulingEngine schedulingEngine;
    private ConflictDetection conflictDetection;
    private CsvDataLoader dataLoader;
    private Constraints constraints;

    private Consumer<Calendar> calendarSink;
    private Consumer<List<Conflict>> conflictsSink;

    private List<Student> students;
    private List<Course> courses;
    private List<Classroom> classrooms;
    private CsvDataLoader.AttendanceData attendanceData;
    private Consumer<String> statusSink;

    public MainController() {
        this.schedulingEngine = new SchedulingEngine();
        this.conflictDetection = new ConflictDetection();
        this.dataLoader = new CsvDataLoader();
        this.constraints = new Constraints();
        this.calendarSink = null;
        this.conflictsSink = null;
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.classrooms = new ArrayList<>();
        this.attendanceData = null;
        this.statusSink = null;
    }

    public void initialize() {
        this.calendar = new Calendar();
        this.constraints = new Constraints();
        this.students.clear();
        this.courses.clear();
        this.classrooms.clear();
        this.attendanceData = null;
        status("MainController initialized with an empty calendar.");
    }

    public void onLoadData() {
        status("onLoadData() called - use onLoadData(studentsPath, coursesPath, classroomsPath, attendanceListPath) from the UI.");
    }

    public void onLoadData(String studentsPath,
                           String coursesPath,
                           String classroomsPath,
                           String attendanceListPath) {
        if (studentsPath == null || studentsPath.isBlank()
                || coursesPath == null || coursesPath.isBlank()
                || classroomsPath == null || classroomsPath.isBlank()
                || attendanceListPath == null || attendanceListPath.isBlank()) {
            status("Load Data failed: missing required CSV path(s)." );
            return;
        }

        status("Loading CSVs..." );
        status("Students CSV: " + studentsPath);
        status("Courses CSV: " + coursesPath);
        status("Classrooms CSV: " + classroomsPath);
        status("Attendance CSV: " + attendanceListPath);

        try {
            this.students = dataLoader.loadStudents(studentsPath);
            this.courses = dataLoader.loadCourses(coursesPath);
            this.classrooms = dataLoader.loadClassrooms(classroomsPath);
            this.attendanceData = dataLoader.loadAttendanceList(attendanceListPath);
            dataLoader.linkAttendance(this.students, this.courses, this.attendanceData);
            // Students
            if (this.students != null) {
                for (Student s : this.students) {
                    System.out.println(s);
                }
            }
            // Courses
            if (this.courses != null) {
                for (Course c : this.courses) {
                    System.out.println(c);
                }
            }
            // Classrooms
            if (this.classrooms != null) {
                for (Classroom r : this.classrooms) {
                    System.out.println(r);
                }
            }
            status("Attendance list loaded and linked.");

            status("Loaded: students=" + (students == null ? 0 : students.size())
                    + ", courses=" + (courses == null ? 0 : courses.size())
                    + ", classrooms=" + (classrooms == null ? 0 : classrooms.size()));
        } catch (Exception ex) {
            status("Load Data failed: " + ex.getMessage());
        }
    }

    public void onGenerateSchedule() {
        if (courses == null || courses.isEmpty()) {
            status("Cannot generate schedule: courses not loaded.");
            return;
        }
        if (classrooms == null || classrooms.isEmpty()) {
            status("Cannot generate schedule: classrooms not loaded.");
            return;
        }
        if (constraints == null) {
            constraints = new Constraints();
        }

        try {
            this.calendar = schedulingEngine.generateSchedule(courses, classrooms, constraints);
            int count = (calendar == null) ? 0 : calendar.getExamSessions().size();
            status("Schedule generated: sessions=" + count);

            if (calendarSink != null && calendar != null) {
                calendarSink.accept(calendar);
            }
        } catch (Exception ex) {
            status("Schedule generation failed: " + ex.getMessage());
        }
    }

    public void onShowConflicts() {
        if (calendar == null) {
            status("No calendar generated yet. Cannot show conflicts.");
            return;
        }

        try {
            List<Conflict> conflicts = conflictDetection.detectConflicts(calendar);
            if (conflicts == null) conflicts = List.of();

            if (conflicts.isEmpty()) {
                status("No conflicts detected.");
            } else {
                status("Conflicts detected: " + conflicts.size());

                java.util.Map<ConflictType, Integer> counts = new java.util.EnumMap<>(ConflictType.class);
                for (Conflict c : conflicts) {
                    ConflictType t = (c == null) ? null : c.getType();
                    if (t != null) counts.put(t, counts.getOrDefault(t, 0) + 1);
                }
                for (java.util.Map.Entry<ConflictType, Integer> e : counts.entrySet()) {
                    status("- " + e.getKey() + ": " + e.getValue());
                }

                int limit = Math.min(10, conflicts.size());
                for (int i = 0; i < limit; i++) {
                    status("#" + (i + 1) + " " + conflicts.get(i));
                }
                if (conflicts.size() > limit) {
                    status("... (" + (conflicts.size() - limit) + " more)");
                }
            }

            if (conflictsSink != null) {
                conflictsSink.accept(conflicts);
            }
        } catch (Exception ex) {
            status("Conflict detection failed: " + ex.getMessage());
        }
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public List<Student> getStudents() {
        return students;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public List<Classroom> getClassrooms() {
        return classrooms;
    }

    public CsvDataLoader.AttendanceData getAttendanceData() {
        return attendanceData;
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
    public void setStatusSink(Consumer<String> statusSink) {
        this.statusSink = statusSink;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public void setCalendarSink(Consumer<Calendar> calendarSink) {
        this.calendarSink = calendarSink;
    }

    public void setConflictsSink(Consumer<List<Conflict>> conflictsSink) {
        this.conflictsSink = conflictsSink;
    }

    public boolean hasLoadedData() {
        return students != null && !students.isEmpty()
                && courses != null && !courses.isEmpty()
                && classrooms != null && !classrooms.isEmpty()
                && attendanceData != null;
    }

    private void status(String message) {
        System.out.println(message);
        if (statusSink != null) {
            statusSink.accept(message);
        }
    }
}
