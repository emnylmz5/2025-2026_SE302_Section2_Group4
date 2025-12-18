package org.example.se302;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConflictDetection {

    public List<Conflict> detectConflicts(Calendar calendar) {
        if (calendar == null) return List.of();

        List<ExamSession> sessions = new ArrayList<>(calendar.getExamSessions());
        List<SessionInfo> info = new ArrayList<>();

        for (ExamSession s : sessions) {
            SessionInfo si = buildInfo(s);
            if (si != null) info.add(si);
        }

        List<Conflict> out = new ArrayList<>();
        out.addAll(detectCapacityConflicts(info));
        out.addAll(detectRoomOverlaps(info));
        out.addAll(detectStudentCollisions(info));
        return out;
    }

    public boolean hasConflicts(Calendar calendar) {
        return !detectConflicts(calendar).isEmpty();
    }

    private List<Conflict> detectCapacityConflicts(List<SessionInfo> sessions) {
        List<Conflict> out = new ArrayList<>();

        for (SessionInfo si : sessions) {
            for (RoomAssign ra : si.roomAssignments) {
                if (ra.room == null) continue;

                Integer cap = classroomCapacityOf(ra.room);
                if (cap == null) continue;

                int assigned = ra.students.size();
                if (assigned > cap) {
                    String roomId = classroomIdOf(ra.room);
                    String desc = "Room capacity exceeded"
                            + (roomId.isEmpty() ? "" : (": " + roomId))
                            + " (assigned=" + assigned + ", capacity=" + cap + ")";

                    Conflict c = new Conflict();
                    c.setType(ConflictType.ROOM_CAPACITY);
                    c.addSession(si.session);
                    c.setDescription(desc);
                    out.add(c);
                }
            }
        }

        return out;
    }

    private List<Conflict> detectRoomOverlaps(List<SessionInfo> sessions) {
        List<Conflict> out = new ArrayList<>();

        for (int i = 0; i < sessions.size(); i++) {
            for (int j = i + 1; j < sessions.size(); j++) {
                SessionInfo a = sessions.get(i);
                SessionInfo b = sessions.get(j);

                if (!overlaps(a, b)) continue;

                Set<Classroom> commonRooms = new HashSet<>(a.rooms);
                commonRooms.retainAll(b.rooms);
                if (commonRooms.isEmpty()) continue;

                for (Classroom r : commonRooms) {
                    String roomId = classroomIdOf(r);
                    String desc = "Room overlap" + (roomId.isEmpty() ? "" : (": " + roomId));

                    Conflict c = new Conflict();
                    c.setType(ConflictType.ROOM_OVERLAP);
                    c.addSession(a.session);
                    c.addSession(b.session);
                    c.setDescription(desc);
                    out.add(c);
                }
            }
        }

        return out;
    }

    private List<Conflict> detectStudentCollisions(List<SessionInfo> sessions) {
        List<Conflict> out = new ArrayList<>();

        for (int i = 0; i < sessions.size(); i++) {
            for (int j = i + 1; j < sessions.size(); j++) {
                SessionInfo a = sessions.get(i);
                SessionInfo b = sessions.get(j);

                if (!overlaps(a, b)) continue;

                Set<Student> common = new HashSet<>(a.allStudents);
                common.retainAll(b.allStudents);
                if (common.isEmpty()) continue;

                for (Student st : common) {
                    String sid = studentIdOf(st);
                    String desc = "Student collision" + (sid.isEmpty() ? "" : (": " + sid));

                    Conflict c = new Conflict();
                    c.setType(ConflictType.STUDENT_COLLISION);
                    c.addSession(a.session);
                    c.addSession(b.session);
                    c.setDescription(desc);
                    out.add(c);
                }
            }
        }

        return out;
    }

    private boolean overlaps(SessionInfo a, SessionInfo b) {
        if (a.start == null || a.end == null || b.start == null || b.end == null) return false;
        return a.start.isBefore(b.end) && b.start.isBefore(a.end);
    }

    private SessionInfo buildInfo(ExamSession s) {
        if (s == null) return null;

        LocalDateTime start = startDateTimeOf(s);
        Integer durMin = durationMinutesOf(s);
        if (start == null || durMin == null) return null;

        LocalDateTime end = start.plusMinutes(durMin);

        List<RoomAssign> ras = extractRoomAssignments(s);
        Set<Classroom> rooms = new HashSet<>();
        Set<Student> students = new HashSet<>();
        for (RoomAssign ra : ras) {
            if (ra.room != null) rooms.add(ra.room);
            students.addAll(ra.students);
        }

        Object all = tryInvokeNoArg(s, "getAllStudents");
        if (all instanceof Iterable<?>) {
            Set<Student> ss = new HashSet<>();
            for (Object o : (Iterable<?>) all) {
                if (o instanceof Student) ss.add((Student) o);
            }
            if (!ss.isEmpty()) students = ss;
        }

        return new SessionInfo(s, start, end, ras, rooms, students);
    }

    private List<RoomAssign> extractRoomAssignments(ExamSession session) {
        Object v = tryInvokeNoArg(session, "getRoomAssignments");
        if (v == null) v = tryGetField(session, "roomAssignments");
        if (!(v instanceof Iterable<?>)) return List.of();

        List<RoomAssign> out = new ArrayList<>();
        for (Object a : (Iterable<?>) v) {
            if (a == null) continue;

            Classroom room = null;
            Object rObj = tryInvokeNoArg(a, "getRoom");
            if (rObj == null) rObj = tryGetField(a, "room");
            if (rObj instanceof Classroom) room = (Classroom) rObj;

            Set<Student> students = new HashSet<>();
            Object sObj = tryInvokeNoArg(a, "getStudents");
            if (sObj == null) sObj = tryGetField(a, "students");
            if (sObj instanceof Iterable<?>) {
                for (Object o : (Iterable<?>) sObj) {
                    if (o instanceof Student) students.add((Student) o);
                }
            }

            out.add(new RoomAssign(room, students));
        }

        return out;
    }

    private LocalDateTime startDateTimeOf(ExamSession s) {
        Object v = tryInvokeNoArg(s, "getStartDateTime");
        if (v == null) v = tryGetField(s, "startDateTime");
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        return null;
    }

    private Integer durationMinutesOf(ExamSession s) {
        Object v = tryInvokeNoArg(s, "getDurationMinutes");
        if (v == null) v = tryGetField(s, "durationMinutes");
        if (v instanceof Number) return ((Number) v).intValue();

        Object d = tryInvokeNoArg(s, "getDuration");
        if (d == null) d = tryGetField(s, "duration");
        if (d instanceof Duration) return (int) ((Duration) d).toMinutes();

        if (v != null) {
            try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
        }
        if (d != null) {
            try { return Integer.parseInt(String.valueOf(d)); } catch (Exception ignored) {}
        }

        return null;
    }

    private String studentIdOf(Student st) {
        if (st == null) return "";
        Object v = tryInvokeNoArg(st, "getStudentId");
        if (v == null) v = tryGetField(st, "studentId");
        return v == null ? "" : String.valueOf(v);
    }

    private String classroomIdOf(Classroom r) {
        if (r == null) return "";
        Object v = tryInvokeNoArg(r, "getClassroomId");
        if (v == null) v = tryInvokeNoArg(r, "getId");
        if (v == null) v = tryGetField(r, "classroomId");
        if (v == null) v = tryGetField(r, "id");
        return v == null ? "" : String.valueOf(v);
    }

    private Integer classroomCapacityOf(Classroom r) {
        if (r == null) return null;
        Object v = tryInvokeNoArg(r, "getCapacity");
        if (v == null) v = tryGetField(r, "capacity");
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) {
            try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
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

    private static final class SessionInfo {
        final ExamSession session;
        final LocalDateTime start;
        final LocalDateTime end;
        final List<RoomAssign> roomAssignments;
        final Set<Classroom> rooms;
        final Set<Student> allStudents;

        SessionInfo(ExamSession session,
                    LocalDateTime start,
                    LocalDateTime end,
                    List<RoomAssign> roomAssignments,
                    Set<Classroom> rooms,
                    Set<Student> allStudents) {
            this.session = session;
            this.start = start;
            this.end = end;
            this.roomAssignments = roomAssignments;
            this.rooms = rooms;
            this.allStudents = allStudents;
        }
    }

    private static final class RoomAssign {
        final Classroom room;
        final Set<Student> students;

        RoomAssign(Classroom room, Set<Student> students) {
            this.room = room;
            this.students = (students == null) ? Set.of() : students;
        }
    }
}