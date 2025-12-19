package org.example.se302;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchedulingEngine {

    private int currentRoomTurnoverMinutes = 0;

    private int getRoomTurnoverMinutes() {
        return Math.max(0, currentRoomTurnoverMinutes);
    }

    private static final int DEFAULT_DURATION_MIN = 120;
    private static final int MAX_SEARCH_DAYS = 90;

    public Calendar generateSchedule(List<Course> courses,
                                     List<Classroom> classrooms,
                                     Constraints constraints) {
        if (courses == null || classrooms == null) {
            throw new IllegalArgumentException("courses/classrooms cannot be null");
        }
        if (constraints == null) {
            constraints = new Constraints();
        }

        List<Course> remaining = new ArrayList<>();
        for (Course c : courses) {
            if (c == null) continue;
            if (extractEnrolledStudents(c).isEmpty()) continue;
            remaining.add(c);
        }

        // Schedule larger courses first to avoid small courses consuming the earliest slots/rooms.
        remaining.sort(Comparator
                .comparingInt((Course c) -> safeSize(extractEnrolledStudents(c)))
                .reversed()
                .thenComparing(c -> safeString(courseCodeOf(c))));

        List<Classroom> orderedRooms = new ArrayList<>(classrooms);
        // Alphabetical by block + room number (e.g., A101, A102 ... M101, M116)
        orderedRooms.sort(Comparator
                .comparing((Classroom r) -> roomBlock(r == null ? null : r.getClassroomId()))
                .thenComparingInt(r -> roomNumber(r == null ? null : r.getClassroomId()))
                .thenComparing(r -> r == null ? "" : r.getClassroomId()));

        Calendar calendar = new Calendar();

        LocalDate startDate = constraints.getExamWeekStartDate();
        if (startDate == null) {
            startDate = LocalDate.now().plusDays(1);
        }

        LocalDate endDate = constraints.getExamWeekEndDate();
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("examWeekEndDate cannot be before examWeekStartDate");
        }

        this.currentRoomTurnoverMinutes = Math.max(0, constraints.getRoomTurnoverMinutes());

        int maxDaysToTry = MAX_SEARCH_DAYS;
        if (endDate != null) {
            long span = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (span <= 0) {
                throw new IllegalArgumentException("Invalid exam week date range");
            }
            maxDaysToTry = (int) Math.min(span, MAX_SEARCH_DAYS);
        }

        // Course-by-course earliest-fit: each course restarts scanning from the beginning.
        for (Course c : remaining) {
            boolean placed = false;

            int duration = estimateDurationMinutes(c, constraints);
            List<Student> enrolled = extractEnrolledStudents(c);
            if (enrolled.isEmpty()) continue;

            for (int dayOffset = 0; dayOffset < maxDaysToTry && !placed; dayOffset++) {
                LocalDate d = startDate.plusDays(dayOffset);
                if (endDate != null && d.isAfter(endDate)) break;
                if (!isAllowedDay(d, constraints)) continue;

                for (LocalDateTime slotStart : candidateStartsForDate(d, constraints)) {
                    if (!fitsTimeRanges(slotStart, duration, constraints)) continue;

                    // Only use rooms that are free at this slot (with turnover buffer)
                    List<Classroom> available = availableRoomsAt(calendar, orderedRooms, slotStart, duration);
                    if (available.isEmpty()) continue;

                    ExamSession candidate = buildSession(c, slotStart, duration, enrolled, available);
                    if (candidate.getRoomAssignments().isEmpty()) continue;

                    if (conflictsWithExisting(calendar, candidate)) continue;
                    if (violatesStudentConstraints(calendar, candidate, constraints)) continue;

                    calendar.addExamSession(candidate);
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                throw new IllegalStateException("Could not schedule course: " + safeString(courseCodeOf(c)));
            }
        }

        return calendar;
    }

    private boolean isAllowedDay(LocalDate d, Constraints constraints) {
        List<DayOfWeek> allowed = constraints.getAllowedDays();
        if (allowed == null || allowed.isEmpty()) return true;
        return allowed.contains(d.getDayOfWeek());
    }

    private List<LocalDateTime> candidateStartsForDate(LocalDate date, Constraints constraints) {
        List<Constraints.TimeRange> ranges = constraints.getAllowedTimeRanges();
        if (ranges == null || ranges.isEmpty()) {
            ranges = List.of(new Constraints.TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0)));
        }

        int step = Math.max(1, constraints.getSlotStepMinutes());

        List<LocalDateTime> out = new ArrayList<>();
        for (Constraints.TimeRange tr : ranges) {
            if (tr == null || tr.getStart() == null || tr.getEnd() == null) continue;

            LocalTime t = tr.getStart();
            // Slot generation is independent of exam durations; duration checks are applied later per course.
            while (!t.isAfter(tr.getEnd())) {
                out.add(date.atTime(t));
                t = t.plusMinutes(step);
            }
        }

        return out;
    }

    private boolean fitsTimeRanges(LocalDateTime start, int durationMinutes, Constraints constraints) {
        if (start == null) return false;

        LocalTime s = start.toLocalTime();
        LocalTime e = s.plusMinutes(durationMinutes);
        List<Constraints.TimeRange> ranges = constraints.getAllowedTimeRanges();

        if (ranges == null || ranges.isEmpty()) {
            return true;
        }

        for (Constraints.TimeRange tr : ranges) {
            if (tr == null || tr.getStart() == null || tr.getEnd() == null) continue;
            if (!s.isBefore(tr.getStart()) && !e.isAfter(tr.getEnd())) return true;
        }

        return false;
    }

    private List<Classroom> availableRoomsAt(Calendar calendar,
                                            List<Classroom> rooms,
                                            LocalDateTime start,
                                            int durationMinutes) {
        if (calendar == null) return rooms;

        int turnover = getRoomTurnoverMinutes();
        LocalDateTime end = start.plusMinutes(durationMinutes);
        LocalDateTime endWithTurnover = end.plusMinutes(turnover);

        List<Classroom> free = new ArrayList<>();
        for (Classroom room : rooms) {
            if (room == null) continue;

            boolean ok = true;
            for (ExamSession ex : calendar.getExamSessions()) {
                if (ex == null) continue;
                if (!sessionUsesRoom(ex, room)) continue;

                LocalDateTime s2 = ex.getStartDateTime();
                if (s2 == null) continue;
                LocalDateTime e2 = s2.plusMinutes(ex.getDurationMinutes()).plusMinutes(turnover);

                boolean overlap = start.isBefore(e2) && s2.isBefore(endWithTurnover);
                if (overlap) {
                    ok = false;
                    break;
                }
            }

            if (ok) free.add(room);
        }

        return free;
    }

    private boolean sessionUsesRoom(ExamSession s, Classroom room) {
        if (s == null || room == null) return false;
        for (ExamRoomAssignment a : s.getRoomAssignments()) {
            if (a != null && room.equals(a.getRoom())) return true;
        }
        return false;
    }

    private ExamSession buildSession(Course course,
                                     LocalDateTime start,
                                     int durationMinutes,
                                     List<Student> enrolled,
                                     List<Classroom> rooms) {
        ExamSession session = new ExamSession(course, start, durationMinutes);

        List<Student> remaining = new ArrayList<>(enrolled);

        // Classroom assignment order is alphabetical (block + number) as provided by the caller.
        for (Classroom room : rooms) {
            if (remaining.isEmpty()) break;
            if (room == null) continue;

            int cap = room.getCapacity();
            if (cap <= 0) continue;

            int take = Math.min(cap, remaining.size());
            List<Student> chunk = new ArrayList<>(remaining.subList(0, take));
            remaining.subList(0, take).clear();

            session.addRoomAssignment(new ExamRoomAssignment(room, chunk));
        }

        if (!remaining.isEmpty()) {
            session.setRoomAssignments(List.of());
        }

        return session;
    }

    private boolean conflictsWithExisting(Calendar calendar, ExamSession candidate) {
        if (calendar == null || candidate == null) return false;

        LocalDateTime s1 = candidate.getStartDateTime();
        LocalDateTime e1 = (s1 == null) ? null : s1.plusMinutes(candidate.getDurationMinutes());
        if (s1 == null || e1 == null) return true;

        Set<Classroom> candRooms = extractRooms(candidate);
        Set<Student> candStudents = new HashSet<>(candidate.getAllStudents());

        for (ExamSession existing : calendar.getExamSessions()) {
            if (existing == null) continue;

            LocalDateTime s2 = existing.getStartDateTime();
            LocalDateTime e2 = (s2 == null) ? null : s2.plusMinutes(existing.getDurationMinutes());
            if (s2 == null || e2 == null) continue;

            boolean overlap = s1.isBefore(e2) && s2.isBefore(e1);

            Set<Classroom> exRooms = extractRooms(existing);
            boolean sharesRoom = false;
            for (Classroom r : candRooms) {
                if (exRooms.contains(r)) {
                    sharesRoom = true;
                    break;
                }
            }

            // Room conflict uses a turnover buffer (e.g., 10 minutes) even if times do not strictly overlap.
            if (sharesRoom) {
                int roomBuffer = getRoomTurnoverMinutes();
                LocalDateTime e2b = e2.plusMinutes(roomBuffer);
                LocalDateTime e1b = e1.plusMinutes(roomBuffer);

                boolean overlapWithBuffer = s1.isBefore(e2b) && s2.isBefore(e1b);
                if (overlapWithBuffer) return true;
            }

            // Student collision only matters if sessions overlap in time.
            if (overlap) {
                Set<Student> exStudents = new HashSet<>(existing.getAllStudents());
                for (Student st : candStudents) {
                    if (exStudents.contains(st)) return true;
                }
            }
        }

        return false;
    }

    private boolean violatesStudentConstraints(Calendar calendar, ExamSession candidate, Constraints constraints) {
        if (calendar == null || candidate == null || constraints == null) return false;

        int minGap = Math.max(0, constraints.getMinMinutesBetweenExams());
        int maxPerDay = Math.max(1, constraints.getMaxExamsPerDay());

        LocalDateTime start = candidate.getStartDateTime();
        if (start == null) return true;
        LocalDate date = start.toLocalDate();
        LocalDateTime end = start.plusMinutes(candidate.getDurationMinutes());

        Map<Student, List<ExamSession>> perStudent = indexSessionsByStudent(calendar);

        for (Student st : candidate.getAllStudents()) {
            List<ExamSession> already = perStudent.getOrDefault(st, List.of());

            int dayCount = 0;
            for (ExamSession ex : already) {
                LocalDateTime s2 = ex.getStartDateTime();
                if (s2 != null && date.equals(s2.toLocalDate())) dayCount++;
            }
            if (dayCount >= maxPerDay) return true;

            for (ExamSession ex : already) {
                LocalDateTime s2 = ex.getStartDateTime();
                if (s2 == null) continue;
                LocalDateTime e2 = s2.plusMinutes(ex.getDurationMinutes());

                boolean overlap = start.isBefore(e2) && s2.isBefore(end);
                if (overlap) return true;

                long gap1 = Duration.between(e2, start).toMinutes();
                long gap2 = Duration.between(end, s2).toMinutes();
                long gap = Math.max(gap1, gap2);

                if (gap >= 0 && gap < minGap) return true;
            }
        }

        return false;
    }

    private Map<Student, List<ExamSession>> indexSessionsByStudent(Calendar calendar) {
        Map<Student, List<ExamSession>> map = new HashMap<>();

        for (ExamSession s : calendar.getExamSessions()) {
            if (s == null) continue;
            for (Student st : s.getAllStudents()) {
                map.computeIfAbsent(st, k -> new ArrayList<>()).add(s);
            }
        }

        return map;
    }

    private Set<Classroom> extractRooms(ExamSession s) {
        Set<Classroom> out = new HashSet<>();
        if (s == null) return out;

        for (ExamRoomAssignment a : s.getRoomAssignments()) {
            if (a != null && a.getRoom() != null) out.add(a.getRoom());
        }
        return out;
    }

    private int estimateDurationMinutes(Course c, Constraints constraints) {
        if (constraints == null) return DEFAULT_DURATION_MIN;

        int base = Math.max(1, constraints.getBaseExamDurationMinutes());
        int k = Math.max(0, constraints.getCreditDurationCoefficientMinutes());

        Integer credit = creditOf(c);
        int raw;
        if (credit == null || credit <= 0) {
            raw = Math.max(DEFAULT_DURATION_MIN, base);
        } else {
            raw = base + (credit * k);
            raw = Math.max(DEFAULT_DURATION_MIN, raw);
        }

        // Optional rounding: round UP to the nearest multiple (e.g., 5 min -> 135, 140, 145 ...)
        int roundTo = Math.max(1, constraints.getDurationRoundingMinutes());
        if (roundTo > 1) {
            raw = ((raw + roundTo - 1) / roundTo) * roundTo;
        }

        return raw;
    }

    private List<Student> extractEnrolledStudents(Course c) {
        Object v = tryInvokeNoArg(c, "getEnrolledStudents");
        if (v == null) v = tryGetField(c, "enrolledStudents");
        if (v == null) v = tryGetField(c, "students");

        if (v instanceof Iterable<?>) {
            List<Student> out = new ArrayList<>();
            for (Object o : (Iterable<?>) v) {
                if (o instanceof Student) out.add((Student) o);
            }
            return out;
        }

        return List.of();
    }

    private String courseCodeOf(Course c) {
        Object v = tryInvokeNoArg(c, "getCourseCode");
        if (v == null) v = tryGetField(c, "courseCode");
        return v == null ? "" : String.valueOf(v);
    }

    private Integer creditOf(Course c) {
        Object v = tryInvokeNoArg(c, "getCredit");
        if (v == null) v = tryGetField(c, "credit");
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) {
            try {
                return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignored) {
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

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private String safeString(String s) {
        return s == null ? "" : s;
    }

    private String roomBlock(String id) {
        if (id == null) return "";
        String letters = id.replaceAll("[^A-Za-z]", "");
        return letters == null ? "" : letters;
    }

    private int roomNumber(String id) {
        if (id == null) return Integer.MAX_VALUE;
        String digits = id.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
