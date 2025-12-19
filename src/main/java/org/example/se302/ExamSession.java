package org.example.se302;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExamSession {

    private Course course;
    private LocalDateTime startDateTime;
    private int durationMinutes;
    private List<ExamRoomAssignment> roomAssignments;

    public ExamSession() {
        this.roomAssignments = new ArrayList<>();
    }

    public ExamSession(Course course, LocalDateTime startDateTime, int durationMinutes) {
        this.course = course;
        this.startDateTime = startDateTime;
        this.durationMinutes = durationMinutes;
        this.roomAssignments = new ArrayList<>();
    }

    public ExamSession(Course course,
                       LocalDateTime startDateTime,
                       int durationMinutes,
                       List<ExamRoomAssignment> roomAssignments) {
        this.course = course;
        this.startDateTime = startDateTime;
        this.durationMinutes = durationMinutes;
        this.roomAssignments = (roomAssignments == null) ? new ArrayList<>() : new ArrayList<>(roomAssignments);
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<ExamRoomAssignment> getRoomAssignments() {
        return roomAssignments == null ? List.of() : Collections.unmodifiableList(roomAssignments);
    }

    public void setRoomAssignments(List<ExamRoomAssignment> roomAssignments) {
        this.roomAssignments = (roomAssignments == null) ? new ArrayList<>() : new ArrayList<>(roomAssignments);
    }

    public void addRoomAssignment(ExamRoomAssignment a) {
        if (a == null) return;
        if (this.roomAssignments == null) this.roomAssignments = new ArrayList<>();
        this.roomAssignments.add(a);
    }

    public void removeRoomAssignment(ExamRoomAssignment a) {
        if (this.roomAssignments != null) {
            this.roomAssignments.remove(a);
        }
    }

    public List<Student> getAllStudents() {
        if (roomAssignments == null || roomAssignments.isEmpty()) return List.of();

        Set<Student> set = new LinkedHashSet<>();
        for (ExamRoomAssignment a : roomAssignments) {
            if (a == null) continue;
            for (Student s : a.getStudents()) {
                if (s != null) set.add(s);
            }
        }
        return new ArrayList<>(set);
    }

    public int getTotalStudentCount() {
        return getAllStudents().size();
    }

    public String getCourseCode() {
        return course == null ? "" : course.getCourseCode();
    }

    @Override
    public String toString() {
        return "ExamSession{" +
                "courseCode='" + getCourseCode() + '\'' +
                ", startDateTime=" + startDateTime +
                ", durationMinutes=" + durationMinutes +
                ", roomAssignments=" + (roomAssignments == null ? 0 : roomAssignments.size()) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamSession that)) return false;
        return durationMinutes == that.durationMinutes
                && Objects.equals(course, that.course)
                && Objects.equals(startDateTime, that.startDateTime)
                && Objects.equals(roomAssignments, that.roomAssignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(course, startDateTime, durationMinutes, roomAssignments);
    }
}

