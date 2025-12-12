package org.example.se302;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Student {

    private String studentId;
    private String name;
    private List<Course> enrolledCourses;

    public Student() {
        this.enrolledCourses = new ArrayList<>();
    }

    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
        this.enrolledCourses = new ArrayList<>();
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Course> getEnrolledCourses() {
        return Collections.unmodifiableList(enrolledCourses);
    }

    public void enrollInCourse(Course course) {
        if (course == null) {
            return;
        }
        if (!enrolledCourses.contains(course)) {
            enrolledCourses.add(course);
        }
    }

    /**
     * Drop this student from the given course.
     * This method only updates the student's side of the relationship.
     */
    public void dropCourse(Course course) {
        if (course == null) {
            return;
        }
        enrolledCourses.remove(course);
    }

    /**
     * Returns the list of exam sessions this student attends.
     * For now this method returns an empty list as a placeholder.
     * It can be updated later to derive sessions from course / calendar data.
     */
    public List<ExamSession> getExamSession() {
        return new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(studentId, student.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
