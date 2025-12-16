
package org.example.se302;

import java.util.Objects;

public class Classroom {

    private String classroomId;
    private int capacity;

    public Classroom() {
    }

    public Classroom(String classroomId, int capacity) {
        this.classroomId = classroomId;
        this.capacity = capacity;
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    // Aliases for CSV/loader flexibility

    public String getId() {
        return classroomId;
    }

    public void setId(String id) {
        this.classroomId = id;
    }

    public String getName() {
        return classroomId;
    }

    public void setName(String name) {
        this.classroomId = name;
    }

    @Override
    public String toString() {
        return "Classroom{" +
                "classroomId='" + classroomId + '\'' +
                ", capacity=" + capacity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Classroom classroom)) return false;
        return Objects.equals(classroomId, classroom.classroomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classroomId);
    }
}
