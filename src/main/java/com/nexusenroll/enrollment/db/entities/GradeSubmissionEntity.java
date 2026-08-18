package com.nexusenroll.enrollment.db.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grade_submissions")
public class GradeSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gradeId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "course_id", nullable = false, length = 36)
    private String courseId;

    // 'A+', 'B', 'C', 'F'
    @Column(nullable = false, length = 5)
    private String letterGrade;

    // 4.00, 3.00, etc.
    @Column(nullable = false)
    private double gradePoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradeStatus status;

    // Soft reference to Faculty submitting the grade
    @Column(name = "submitted_by", nullable = false, length = 36)
    private String submittedByFacultyId;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    public GradeSubmissionEntity() {
    }

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public double getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(double gradePoint) {
        this.gradePoint = gradePoint;
    }

    public GradeStatus getStatus() {
        return status;
    }

    public void setStatus(GradeStatus status) {
        this.status = status;
    }

    public String getSubmittedByFacultyId() {
        return submittedByFacultyId;
    }

    public void setSubmittedByFacultyId(String submittedByFacultyId) {
        this.submittedByFacultyId = submittedByFacultyId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}