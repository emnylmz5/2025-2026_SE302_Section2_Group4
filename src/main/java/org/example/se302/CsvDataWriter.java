
package org.example.se302;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvDataWriter {

    public void writeStudents(List<Student> students, String filePath) throws IOException {
        Path p = Path.of(filePath);
        ensureParentDir(p);

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write("studentId,name");
            w.newLine();

            if (students == null) return;
            for (Student s : students) {
                String id = readString(s,
                        new String[]{"getStudentId", "getId"},
                        new String[]{"studentId", "id"});
                String name = readString(s,
                        new String[]{"getName", "getFullName"},
                        new String[]{"name", "fullName"});

                w.write(csv(id));
                w.write(',');
                w.write(csv(name));
                w.newLine();
            }
        }
    }

    public void writeCourses(List<Course> courses, String filePath) throws IOException {
        Path p = Path.of(filePath);
        ensureParentDir(p);

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write("courseCode,name,term,credit");
            w.newLine();

            if (courses == null) return;
            for (Course c : courses) {
                String code = readString(c,
                        new String[]{"getCourseCode", "getCode"},
                        new String[]{"courseCode", "code"});

                String name = readString(c,
                        new String[]{"getCourseName", "getName"},
                        new String[]{"courseName", "name"});

                String term = readString(c,
                        new String[]{"getTerm"},
                        new String[]{"term"});

                String credit = readString(c,
                        new String[]{"getCredit"},
                        new String[]{"credit"});

                w.write(csv(code));
                w.write(',');
                w.write(csv(name));
                w.write(',');
                w.write(csv(term));
                w.write(',');
                w.write(csv(credit));
                w.newLine();
            }
        }
    }

    public void writeClassrooms(List<Classroom> classrooms, String filePath) throws IOException {
        Path p = Path.of(filePath);
        ensureParentDir(p);

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write("classroomId,capacity");
            w.newLine();

            if (classrooms == null) return;
            for (Classroom r : classrooms) {
                String id = readString(r,
                        new String[]{"getClassroomId", "getId"},
                        new String[]{"classroomId", "id"});

                String cap = readString(r,
                        new String[]{"getCapacity"},
                        new String[]{"capacity"});

                w.write(csv(id));
                w.write(',');
                w.write(csv(cap));
                w.newLine();
            }
        }
    }

    /**
     * Attendance list is stored inside Student objects as enrolledCourses.
     * Output format: studentId,courseCode
     */
    public void writeAttendanceFromStudents(List<Student> students, String filePath) throws IOException {
        Path p = Path.of(filePath);
        ensureParentDir(p);

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write("studentId,courseCode");
            w.newLine();

            if (students == null) return;

            for (Student s : students) {
                if (s == null) continue;

                String sid = readString(s,
                        new String[]{"getStudentId", "getId"},
                        new String[]{"studentId", "id"});
                if (sid == null) sid = "";

                Set<String> codes = extractEnrolledCourseCodes(s);
                for (String code : codes) {
                    if (code == null || code.isBlank()) continue;
                    w.write(csv(sid));
                    w.write(',');
                    w.write(csv(code));
                    w.newLine();
                }
            }
        }
    }

    public void writeExamSchedule(Calendar calendar, String filePath) throws IOException {
        Path p = Path.of(filePath);
        ensureParentDir(p);

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write("courseCode,startDateTime,durationMinutes,roomId,studentIds");
            w.newLine();

            if (calendar == null) return;

            for (ExamSession s : calendar.getExamSessions()) {
                if (s == null) continue;

                String courseCode = readString(s,
                        new String[]{"getCourseCode"},
                        new String[]{"courseCode"});

                Object cObj = tryInvokeNoArg(s, "getCourse");
                if ((courseCode == null || courseCode.isBlank()) && cObj != null) {
                    courseCode = readString(cObj,
                            new String[]{"getCourseCode", "getCode"},
                            new String[]{"courseCode", "code"});
                }

                LocalDateTime start = readLocalDateTime(s,
                        new String[]{"getStartDateTime"},
                        new String[]{"startDateTime"});

                String duration = readString(s,
                        new String[]{"getDurationMinutes"},
                        new String[]{"durationMinutes"});

                Object assigns = tryInvokeNoArg(s, "getRoomAssignments");
                if (assigns == null) assigns = tryGetField(s, "roomAssignments");

                if (assigns instanceof Iterable<?>) {
                    for (Object a : (Iterable<?>) assigns) {
                        if (a == null) continue;

                        Object roomObj = tryInvokeNoArg(a, "getRoom");
                        if (roomObj == null) roomObj = tryGetField(a, "room");

                        String roomId = readString(roomObj,
                                new String[]{"getClassroomId", "getId"},
                                new String[]{"classroomId", "id"});

                        Object studentsObj = tryInvokeNoArg(a, "getStudents");
                        if (studentsObj == null) studentsObj = tryGetField(a, "students");

                        String ids = joinStudentIds(studentsObj);

                        w.write(csv(courseCode));
                        w.write(',');
                        w.write(csv(start == null ? "" : start.toString()));
                        w.write(',');
                        w.write(csv(duration));
                        w.write(',');
                        w.write(csv(roomId));
                        w.write(',');
                        w.write(csv(ids));
                        w.newLine();
                    }
                } else {
                    w.write(csv(courseCode));
                    w.write(',');
                    w.write(csv(start == null ? "" : start.toString()));
                    w.write(',');
                    w.write(csv(duration));
                    w.write(',');
                    w.write(csv(""));
                    w.write(',');
                    w.write(csv(""));
                    w.newLine();
                }
            }
        }
    }

    public void writeConflicts(List<Conflict> conflicts, String filePath) throws IOException {
        Path p = Path.of(filePath);
        ensureParentDir(p);

        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            w.write("type,description,sessions");
            w.newLine();

            if (conflicts == null) return;
            for (Conflict c : conflicts) {
                if (c == null) continue;

                String type = readString(c,
                        new String[]{"getType"},
                        new String[]{"type"});

                String desc = readString(c,
                        new String[]{"getDescription"},
                        new String[]{"description"});

                Object sessionsObj = tryInvokeNoArg(c, "getSessions");
                if (sessionsObj == null) sessionsObj = tryGetField(c, "sessions");

                String sessions = joinSessionCodes(sessionsObj);

                w.write(csv(type));
                w.write(',');
                w.write(csv(desc));
                w.write(',');
                w.write(csv(sessions));
                w.newLine();
            }
        }
    }

    private Set<String> extractEnrolledCourseCodes(Student student) {
        Set<String> out = new HashSet<>();
        if (student == null) return out;

        Object v = tryInvokeNoArg(student, "getEnrolledCourses");
        if (v == null) v = tryInvokeNoArg(student, "getEnrolledCourseCodes");
        if (v == null) v = tryGetField(student, "enrolledCourses");

        if (!(v instanceof Iterable<?>)) return out;

        for (Object item : (Iterable<?>) v) {
            if (item == null) continue;

            if (item instanceof String) {
                String code = String.valueOf(item).trim();
                if (!code.isEmpty()) out.add(code);
                continue;
            }

            // If enrolledCourses holds Course objects
            String code = readString(item,
                    new String[]{"getCourseCode", "getCode"},
                    new String[]{"courseCode", "code"});
            if (code != null && !code.isBlank()) out.add(code.trim());
        }

        return out;
    }

    private String joinStudentIds(Object studentsObj) {
        if (!(studentsObj instanceof Iterable<?>)) return "";

        List<String> ids = new ArrayList<>();
        for (Object o : (Iterable<?>) studentsObj) {
            if (o == null) continue;
            String sid = readString(o,
                    new String[]{"getStudentId", "getId"},
                    new String[]{"studentId", "id"});
            if (sid != null && !sid.isBlank()) ids.add(sid);
        }

        return String.join("|", ids);
    }

    private String joinSessionCodes(Object sessionsObj) {
        if (!(sessionsObj instanceof Iterable<?>)) return "";

        List<String> parts = new ArrayList<>();
        for (Object s : (Iterable<?>) sessionsObj) {
            if (s == null) continue;

            String code = readString(s,
                    new String[]{"getCourseCode"},
                    new String[]{"courseCode"});
            Object courseObj = tryInvokeNoArg(s, "getCourse");
            if ((code == null || code.isBlank()) && courseObj != null) {
                code = readString(courseObj,
                        new String[]{"getCourseCode", "getCode"},
                        new String[]{"courseCode", "code"});
            }

            LocalDateTime start = readLocalDateTime(s,
                    new String[]{"getStartDateTime"},
                    new String[]{"startDateTime"});

            if (start != null && code != null && !code.isBlank()) {
                parts.add(code + "@" + start);
            } else if (code != null && !code.isBlank()) {
                parts.add(code);
            } else {
                parts.add(String.valueOf(s));
            }
        }

        return String.join(";", parts);
    }

    private void ensureParentDir(Path p) throws IOException {
        Path parent = p.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value;
        boolean mustQuote = v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"");
        if (v.contains("\"")) {
            v = v.replace("\"", "\"\"");
        }
        return mustQuote ? ("\"" + v + "\"") : v;
    }

    private String readString(Object obj, String[] getterNames, String[] fieldNames) {
        if (obj == null) return "";

        if (getterNames != null) {
            for (String g : getterNames) {
                Object v = tryInvokeNoArg(obj, g);
                if (v != null) return String.valueOf(v);
            }
        }

        if (fieldNames != null) {
            for (String f : fieldNames) {
                Object v = tryGetField(obj, f);
                if (v != null) return String.valueOf(v);
            }
        }

        return "";
    }

    private LocalDateTime readLocalDateTime(Object obj, String[] getterNames, String[] fieldNames) {
        if (obj == null) return null;

        if (getterNames != null) {
            for (String g : getterNames) {
                Object v = tryInvokeNoArg(obj, g);
                if (v instanceof LocalDateTime) return (LocalDateTime) v;
            }
        }

        if (fieldNames != null) {
            for (String f : fieldNames) {
                Object v = tryGetField(obj, f);
                if (v instanceof LocalDateTime) return (LocalDateTime) v;
            }
        }

        return null;
    }

    private Object tryInvokeNoArg(Object target, String methodName) {
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
