
package org.example.se302;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ExamRoomAssignment {

    private Classroom room;
    private List<Student> students;

    public ExamRoomAssignment() {
        this.students = new ArrayList<>();
    }

    public ExamRoomAssignment(Classroom room) {
        this.room = room;
        this.students = new ArrayList<>();
    }

    public ExamRoomAssignment(Classroom room, List<Student> students) {
        this.room = room;
        this.students = (students == null) ? new ArrayList<>() : new ArrayList<>(students);
    }

    public Classroom getRoom() {
        return room;
    }

    public void setRoom(Classroom room) {
        this.room = room;
    }

    public List<Student> getStudents() {
        return students == null ? List.of() : Collections.unmodifiableList(students);
    }

    public void setStudents(List<Student> students) {
        this.students = (students == null) ? new ArrayList<>() : new ArrayList<>(students);
    }

    public int getStudentCount() {
        return students == null ? 0 : students.size();
    }

    public boolean isOverCapacity() {
        if (room == null) return false;
        int cap = room.getCapacity();
        return getStudentCount() > cap;
    }

    @Override
    public String toString() {
        String rid = (room == null) ? "" : room.getClassroomId();
        return "ExamRoomAssignment{" +
                "room='" + rid + '\'' +
                ", students=" + getStudentCount() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamRoomAssignment that)) return false;
        return Objects.equals(room, that.room) && Objects.equals(students, that.students);
    }

    @Override
    public int hashCode() {
        return Objects.hash(room, students);
    }
}
