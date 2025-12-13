package org.example.se302;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CsvDataLoader {
    public static class AttendanceData {
        private final Map<String, List<String>> studentToCourses = new LinkedHashMap<>();

        public Map<String, List<String>> getStudentToCourses() {
            return studentToCourses;
        }

        public void add(String studentId, String courseCode) {
            if (studentId == null || studentId.isBlank() || courseCode == null || courseCode.isBlank()) return;
            studentToCourses.computeIfAbsent(studentId.trim(), k -> new ArrayList<>()).add(courseCode.trim());
        }
    }

    public List<Student> loadStudents(String csvPath) {
        return loadObjects(csvPath, Student.class);
    }

    public List<Course> loadCourses(String csvPath) {
        return loadObjects(csvPath, Course.class);
    }

    public List<Classroom> loadClassrooms(String csvPath) {
        return loadObjects(csvPath, Classroom.class);
    }

    public AttendanceData loadAttendanceList(String csvPath) {
        AttendanceData out = new AttendanceData();

        CsvTable table = readCsv(csvPath);
        if (table.rows.isEmpty()) return out;

        // Heuristic column picking
        int studentCol = findColumn(table.headers, "studentid", "student_id", "student", "sid", "id");
        int courseCol = findColumn(table.headers, "coursecode", "course_code", "course", "cid", "code");

        // If no header match, fallback to 0/1
        if (studentCol < 0) studentCol = 0;
        if (courseCol < 0) courseCol = (table.headers.size() > 1 ? 1 : 0);

        for (List<String> r : table.rows) {
            String sid = getCell(r, studentCol);
            String cc = getCell(r, courseCol);
            out.add(sid, cc);
        }

        return out;
    }

    public Constraints loadConstraints(String csvPath) {
        CsvTable table = readCsv(csvPath);
        if (table.rows.isEmpty()) return null;

        Constraints constraints = newInstanceOrNull(Constraints.class);
        if (constraints == null) return null;

        int keyCol = findColumn(table.headers, "key");
        int valueCol = findColumn(table.headers, "value", "val");

        if (keyCol >= 0 && valueCol >= 0) {
            for (List<String> r : table.rows) {
                String key = getCell(r, keyCol);
                String val = getCell(r, valueCol);
                if (key == null || key.isBlank()) continue;
                setByName(constraints, key, val);
            }
            return constraints;
        }

        // Header-as-properties (use first data row)
        List<String> first = table.rows.get(0);
        for (int i = 0; i < table.headers.size() && i < first.size(); i++) {
            String h = table.headers.get(i);
            String v = first.get(i);
            if (h == null || h.isBlank()) continue;
            setByName(constraints, h, v);
        }

        return constraints;
    }

    public void linkAttendance(List<Student> students, List<Course> courses, AttendanceData attendance) {
        if (students == null || courses == null || attendance == null) return;

        Map<String, Student> studentById = new HashMap<>();
        for (Student s : students) {
            String id = readPossibleId(s);
            if (id != null) studentById.put(id, s);
        }

        Map<String, Course> courseByCode = new HashMap<>();
        for (Course c : courses) {
            String code = readPossibleCode(c);
            if (code != null) courseByCode.put(code, c);
        }

        for (Map.Entry<String, List<String>> e : attendance.getStudentToCourses().entrySet()) {
            Student s = studentById.get(e.getKey());
            if (s == null) continue;

            for (String code : e.getValue()) {
                Course c = courseByCode.get(code);
                if (c == null) continue;

                // Student side
                if (!tryInvoke(s, "addCourse", c)
                        && !tryInvoke(s, "enroll", c)
                        && !tryInvoke(s, "enrollInCourse", c)
                        && !tryInvoke(s, "addEnrolledCourse", c)) {
                    // Try collection fields
                    tryAddToCollectionField(s, Arrays.asList("courses", "enrolledCourses", "courseList"), c);
                }

                // Course side
                if (!tryInvoke(c, "addStudent", s)
                        && !tryInvoke(c, "registerStudent", s)
                        && !tryInvoke(c, "enrollStudent", s)) {
                    tryAddToCollectionField(c, Arrays.asList("students", "registeredStudents", "studentList"), s);
                }
            }
        }
    }

    // -------------------- Generic object loading --------------------

    private <T> List<T> loadObjects(String csvPath, Class<T> clazz) {
        CsvTable table = readCsv(csvPath);
        List<T> out = new ArrayList<>();
        if (table.rows.isEmpty()) return out;

        for (List<String> row : table.rows) {
            T obj = newInstanceOrNull(clazz);
            if (obj == null) continue;

            for (int i = 0; i < table.headers.size() && i < row.size(); i++) {
                String header = table.headers.get(i);
                if (header == null || header.isBlank()) continue;
                String value = row.get(i);
                setByName(obj, header, value);
            }

            out.add(obj);
        }

        return out;
    }

    // -------------------- CSV reading --------------------
    private static class CsvTable {
        final List<String> headers;
        final List<List<String>> rows;

        CsvTable(List<String> headers, List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }

    private CsvTable readCsv(String csvPath) {
        Path p = Path.of(csvPath);
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("CSV file not found: " + csvPath);
        }

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String firstLine = br.readLine();
            if (firstLine == null) return new CsvTable(List.of(), List.of());

            List<String> headers = parseCsvLine(firstLine);
            List<List<String>> rows = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cols = parseCsvLine(line);
                rows.add(cols);
            }

            return new CsvTable(headers, rows);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV: " + csvPath, e);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                // handle escaped quote ""
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == ',' && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }

        out.add(cur.toString().trim());
        return out;
    }

    private static String getCell(List<String> row, int idx) {
        if (row == null || idx < 0 || idx >= row.size()) return null;
        String v = row.get(idx);
        return v == null ? null : v.trim();
    }

    private static int findColumn(List<String> headers, String... candidates) {
        if (headers == null || headers.isEmpty()) return -1;
        Set<String> cand = new HashSet<>();
        for (String c : candidates) cand.add(norm(c));

        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            if (h == null) continue;
            if (cand.contains(norm(h))) return i;
        }
        return -1;
    }

    // -------------------- Reflection mapping --------------------

    private static <T> T newInstanceOrNull(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void setByName(Object target, String headerOrKey, String rawValue) {
        if (target == null) return;
        String key = norm(headerOrKey);
        if (key.isEmpty()) return;

        // 1) Try setter: setXxx
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().startsWith("set") || m.getParameterCount() != 1) continue;
            String prop = norm(m.getName().substring(3));
            if (!prop.equals(key)) continue;

            Class<?> t = m.getParameterTypes()[0];
            Object converted = convert(rawValue, t);
            try {
                m.invoke(target, converted);
            } catch (Exception ignored) {
            }
            return;
        }

        // 2) Try field
        for (Field f : getAllFields(target.getClass())) {
            String fn = norm(f.getName());
            if (!fn.equals(key)) continue;
            Object converted = convert(rawValue, f.getType());
            try {
                f.setAccessible(true);
                f.set(target, converted);
            } catch (Exception ignored) {
            }
            return;
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> out = new ArrayList<>();
        Class<?> cur = type;
        while (cur != null && cur != Object.class) {
            out.addAll(Arrays.asList(cur.getDeclaredFields()));
            cur = cur.getSuperclass();
        }
        return out;
    }

    private static Object convert(String raw, Class<?> targetType) {
        String v = raw == null ? "" : raw.trim();

        try {
            if (targetType == String.class) return v;
            if (targetType == int.class || targetType == Integer.class) return v.isEmpty() ? 0 : Integer.parseInt(v);
            if (targetType == long.class || targetType == Long.class) return v.isEmpty() ? 0L : Long.parseLong(v);
            if (targetType == double.class || targetType == Double.class) return v.isEmpty() ? 0d : Double.parseDouble(v);
            if (targetType == boolean.class || targetType == Boolean.class) {
                if (v.isEmpty()) return false;
                String n = v.toLowerCase(Locale.ROOT);
                return n.equals("true") || n.equals("1") || n.equals("yes") || n.equals("y");
            }
            if (Enum.class.isAssignableFrom(targetType)) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> e = (Class<? extends Enum>) targetType;
                for (Object c : e.getEnumConstants()) {
                    if (norm(((Enum<?>) c).name()).equals(norm(v))) return c;
                }
                // fallback: try valueOf
                return Enum.valueOf(e, v);
            }
        } catch (Exception ignored) {
        }

        // Unknown target type: keep as-is if possible
        return v;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private static boolean tryInvoke(Object target, String methodName, Object arg) {
        if (target == null || methodName == null) return false;
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(methodName) || m.getParameterCount() != 1) continue;
            try {
                if (arg == null || m.getParameterTypes()[0].isAssignableFrom(arg.getClass())) {
                    m.invoke(target, arg);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static void tryAddToCollectionField(Object target, List<String> fieldCandidates, Object element) {
        if (target == null || element == null) return;
        Set<String> cand = new HashSet<>();
        for (String c : fieldCandidates) cand.add(norm(c));

        for (Field f : getAllFields(target.getClass())) {
            if (!Collection.class.isAssignableFrom(f.getType())) continue;
            if (!cand.contains(norm(f.getName()))) continue;
            try {
                f.setAccessible(true);
                Object col = f.get(target);
                if (col instanceof Collection<?> c) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> cc = (Collection<Object>) c;
                    cc.add(element);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static String readPossibleId(Object student) {
        if (student == null) return null;
        // Try getters/fields: id, studentId
        String v = readStringProp(student, Arrays.asList("getStudentId", "getId"), Arrays.asList("studentId", "id"));
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String readPossibleCode(Object course) {
        if (course == null) return null;
        String v = readStringProp(course, Arrays.asList("getCourseCode", "getCode", "getId"), Arrays.asList("courseCode", "code", "id"));
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String readStringProp(Object target, List<String> getterNames, List<String> fieldNames) {
        // getters
        for (String g : getterNames) {
            try {
                Method m = target.getClass().getMethod(g);
                Object val = m.invoke(target);
                if (val != null) return val.toString();
            } catch (Exception ignored) {
            }
        }

        // fields
        Set<String> cand = new HashSet<>();
        for (String f : fieldNames) cand.add(norm(f));
        for (Field f : getAllFields(target.getClass())) {
            if (!cand.contains(norm(f.getName()))) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(target);
                if (val != null) return val.toString();
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}