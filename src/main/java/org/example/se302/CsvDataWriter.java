
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
import java.util.List;

public class CsvDataWriter {

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
