package org.example.se302;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Calendar {

    private final List<ExamSession> examSessions = new ArrayList<>();

    public Calendar() {
    }

    public List<ExamSession> getExamSessions() {
        return Collections.unmodifiableList(examSessions);
    }

    public void addExamSession(ExamSession s) {
        if (s == null) return;
        examSessions.add(s);
    }

    public void removeExamSession(ExamSession s) {
        examSessions.remove(s);
    }

    public List<ExamSession> getSessionsByDate(LocalDate d) {
        if (d == null) return List.of();

        List<ExamSession> out = new ArrayList<>();
        for (ExamSession s : examSessions) {
            LocalDateTime start = extractStartDateTime(s);
            if (start != null && d.equals(start.toLocalDate())) {
                out.add(s);
            }
        }
        return out;
    }

    public List<ExamSession> getSessionsByRoom(Classroom r) {
        if (r == null) return List.of();

        List<ExamSession> out = new ArrayList<>();
        for (ExamSession s : examSessions) {
            if (sessionUsesRoom(s, r)) {
                out.add(s);
            }
        }
        return out;
    }

    public List<ExamSession> getSessionsByStudent(Student st) {
        if (st == null) return List.of();

        List<ExamSession> out = new ArrayList<>();
        for (ExamSession s : examSessions) {
            if (sessionHasStudent(s, st)) {
                out.add(s);
            }
        }
        return out;
    }

    private LocalDateTime extractStartDateTime(ExamSession s) {
        if (s == null) return null;

        try {
            Method m = s.getClass().getMethod("getStartDateTime");
            Object v = m.invoke(s);
            if (v instanceof LocalDateTime) return (LocalDateTime) v;
        } catch (Exception ignored) {
        }

        try {
            Field f = s.getClass().getDeclaredField("startDateTime");
            f.setAccessible(true);
            Object v = f.get(s);
            if (v instanceof LocalDateTime) return (LocalDateTime) v;
        } catch (Exception ignored) {
        }

        return null;
    }

    private boolean sessionUsesRoom(ExamSession session, Classroom room) {
        Object assignments = extractRoomAssignments(session);
        if (!(assignments instanceof Iterable<?>)) return false;

        for (Object a : (Iterable<?>) assignments) {
            if (a == null) continue;

            Object rObj = null;
            try {
                Method m = a.getClass().getMethod("getRoom");
                rObj = m.invoke(a);
            } catch (Exception ignored) {
            }

            if (rObj == null) {
                try {
                    Field f = a.getClass().getDeclaredField("room");
                    f.setAccessible(true);
                    rObj = f.get(a);
                } catch (Exception ignored) {
                }
            }

            if (room.equals(rObj)) return true;
        }

        return false;
    }

    private boolean sessionHasStudent(ExamSession session, Student st) {
        if (session == null || st == null) return false;

        try {
            Method m = session.getClass().getMethod("getAllStudents");
            Object v = m.invoke(session);
            if (v instanceof Iterable<?>) {
                for (Object o : (Iterable<?>) v) {
                    if (st.equals(o)) return true;
                }
            }
        } catch (Exception ignored) {
        }

        Object assignments = extractRoomAssignments(session);
        if (!(assignments instanceof Iterable<?>)) return false;

        for (Object a : (Iterable<?>) assignments) {
            if (a == null) continue;

            Object studentsObj = null;
            try {
                Method m = a.getClass().getMethod("getStudents");
                studentsObj = m.invoke(a);
            } catch (Exception ignored) {
            }

            if (studentsObj == null) {
                try {
                    Field f = a.getClass().getDeclaredField("students");
                    f.setAccessible(true);
                    studentsObj = f.get(a);
                } catch (Exception ignored) {
                }
            }

            if (studentsObj instanceof Iterable<?>) {
                for (Object o : (Iterable<?>) studentsObj) {
                    if (st.equals(o)) return true;
                }
            }
        }

        return false;
    }

    private Object extractRoomAssignments(ExamSession session) {
        if (session == null) return null;

        try {
            Method m = session.getClass().getMethod("getRoomAssignments");
            return m.invoke(session);
        } catch (Exception ignored) {
        }

        try {
            Field f = session.getClass().getDeclaredField("roomAssignments");
            f.setAccessible(true);
            return f.get(session);
        } catch (Exception ignored) {
        }

        return null;
    }

    @Override
    public String toString() {
        return "Calendar{" +
                "examSessions=" + examSessions.size() +
                '}';
    }
}
