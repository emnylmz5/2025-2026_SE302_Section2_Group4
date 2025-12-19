package org.example.se302;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Course {

    private String courseCode;
    private String courseName;
    private int credit;
    private List<Student> enrolledStudents;

    public Course() {
        this.enrolledStudents = new ArrayList<>();
    }

    public Course(String courseCode, String courseName, int credit) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credit = credit;
        this.enrolledStudents = new ArrayList<>();
    }

    // --- Core getters/setters ---

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getCredit() {
        return credit;
    }

    public void setCredit(int credit) {
        this.credit = credit;
    }

    public List<Student> getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(List<Student> enrolledStudents) {
        this.enrolledStudents = (enrolledStudents == null) ? new ArrayList<>() : enrolledStudents;
    }

    // --- Aliases for CSV/loader flexibility ---

    public String getName() {
        return courseName;
    }

    public void setName(String name) {
        this.courseName = name;
    }

    public String getCode() {
        return courseCode;
    }

    public void setCode(String code) {
        this.courseCode = code;
    }

    // --- Enrollment ops ---
    public void addStudent(Student s) {
        if (s == null) return;
        if (enrolledStudents == null) enrolledStudents = new ArrayList<>();
        if (!enrolledStudents.contains(s)) {
            enrolledStudents.add(s);
        }

        // Best-effort bidirectional link (no hard dependency on method name)
        tryInvokeStudent(s, "enrollInCourse", this);
        tryInvokeStudent(s, "addCourse", this);
    }

    public void removeStudent(Student s) {
        if (s == null) return;
        if (enrolledStudents != null) {
            enrolledStudents.remove(s);
        }

        // Best-effort bidirectional unlink
        tryInvokeStudent(s, "dropCourse", this);
        tryInvokeStudent(s, "removeCourse", this);
    }

    private void tryInvokeStudent(Student student, String methodName, Course arg) {
        try {
            var m = student.getClass().getMethod(methodName, Course.class);
            m.invoke(student, arg);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String toString() {
        int count = (enrolledStudents == null) ? 0 : enrolledStudents.size();
        return "Course{" +
                "courseCode='" + courseCode + '\'' +
                ", courseName='" + courseName + '\'' +
                ", credit=" + credit +
                ", enrolledStudents=" + count +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course course)) return false;
        return Objects.equals(courseCode, course.courseCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode);
    }
}
