package com.nexusenroll.enrollment.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;

import com.nexusenroll.enrollment.client.CourseScheduleDTO;
import com.nexusenroll.enrollment.client.CourseServiceClient;
import com.nexusenroll.enrollment.db.EnrollmentRepository;
import com.nexusenroll.enrollment.db.entities.EnrollmentEntity;
import com.nexusenroll.enrollment.db.entities.EnrollmentStatus;
import com.nexusenroll.enrollment.db.entities.WaitlistEntity;
import com.nexusenroll.enrollment.db.entities.WaitlistStatus;

@Singleton
public class EnrollmentOrchestrator {

    @Inject
    private EnrollmentRepository repository;

    @Inject
    private CourseServiceClient courseClient;

    public EnrollmentResult processStudentEnrollment(String studentId, String courseId) {

        try {
            if (repository.isStudentAlreadyEnrolled(studentId, courseId)) {
                // Fail fast: Do not touch the Course Service.
                return new EnrollmentResult(false,
                        "You are already enrolled in or currently processing an enrollment for this course.", 409);
            }
        } catch (Exception e) {
            // throw new RuntimeException("Failed to verify existing enrollments.", e);
        }

        // ==========================================
        // STEP 1: PREREQUISITE VALIDATION
        // ==========================================
        try {

            List<String> requiredPrereqs = courseClient.getPrerequisites(courseId);

            for (String prereqId : requiredPrereqs) {
                boolean hasPassed = repository.hasPassedCourse(studentId, prereqId);
                if (!hasPassed) {
                    // Fail fast: Do not touch the DB or Course Service capacity.
                    return new EnrollmentResult(false,
                            "Missing or failed prerequisite: " + prereqId, 400); // 400 Bad Request
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate prerequisites with Course Service.", e);
        }
        // ==========================================
        // STEP 2: TIME CONFLICT VALIDATION
        // ==========================================
        try {
            // Fetch the schedule for the requested course
            CourseScheduleDTO newCourseSchedule = courseClient.getCourseSchedule(courseId);

            // Only check if the new course actually has a scheduled time
            if (newCourseSchedule != null && newCourseSchedule.getStartTime() != null) {

                // Get all active courses the student is currently taking
                List<EnrollmentEntity> activeEnrollments = repository.findActiveScheduleByStudent(studentId);

                for (EnrollmentEntity activeEnrollment : activeEnrollments) {
                    CourseScheduleDTO existingSchedule = courseClient.getCourseSchedule(activeEnrollment.getCourseId());

                    if (hasTimeConflict(newCourseSchedule, existingSchedule)) {
                        // Fail fast: Return a 409 Conflict indicating a schedule overlap
                        return new EnrollmentResult(false,
                                "Time conflict detected with already-enrolled course: "
                                        + activeEnrollment.getCourseId(),
                                409);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate course schedules.", e);
        }

        // ==========================================
        // STEP 2: INITIALIZE LOCAL SAGA TRANSACTION
        // ==========================================
        EnrollmentEntity enrollment = new EnrollmentEntity();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setEnrolledAt(LocalDateTime.now());
        repository.saveEnrollment(enrollment);

        // ==========================================
        // STEP 3: INTER-SERVICE CAPACITY RESERVATION
        // ==========================================
        try {
            int statusCode = courseClient.reserveSeat(courseId);

            if (statusCode == 200 || statusCode == 201) {
                // SUCCESS: Update local DB
                updateEnrollmentStatus(enrollment, EnrollmentStatus.ENROLLED);
                return new EnrollmentResult(true, "Successfully enrolled in course!", 201); // 201 Created

            } else if (statusCode == 409) {
                // WAITLIST: Course is full
                updateEnrollmentStatus(enrollment, EnrollmentStatus.REJECTED);
                addToWaitlist(studentId, courseId);
                return new EnrollmentResult(true, "Course is full. Added to waitlist.", 202); // 202 Accepted

            } else {
                throw new RuntimeException("Course Service returned unexpected status: " + statusCode);
            }

        } catch (Exception e) {
            // ==========================================
            // STEP 4: SAGA COMPENSATING TRANSACTION
            // ==========================================
            updateEnrollmentStatus(enrollment, EnrollmentStatus.REJECTED);
            courseClient.releaseSeat(courseId);

            throw new RuntimeException("Enrollment process failed. System has been rolled back safely.", e);
        }
    }

    private void updateEnrollmentStatus(EnrollmentEntity enrollment, EnrollmentStatus newStatus) {
        enrollment.setStatus(newStatus);
        repository.updateEnrollment(enrollment);
    }

    private void addToWaitlist(String studentId, String courseId) {
        WaitlistEntity waitlist = new WaitlistEntity();
        waitlist.setStudentId(studentId);
        waitlist.setCourseId(courseId);
        waitlist.setStatus(WaitlistStatus.WAITING);
        waitlist.setQueuePosition(repository.getWaitlistCountForCourse(courseId) + 1);
        repository.saveWaitlist(waitlist);
    }

    /**
     * Checks if two course schedules overlap in days and times.
     */
    private boolean hasTimeConflict(CourseScheduleDTO schedule1, CourseScheduleDTO schedule2) {
        // If either course lacks schedule info, they can't conflict
        if (schedule1 == null || schedule2 == null ||
                schedule1.getScheduleDay() == null || schedule2.getScheduleDay() == null ||
                schedule1.getStartTime() == null || schedule2.getStartTime() == null) {
            return false;
        }

        // Check if they occur on the same day
        if (schedule1.getScheduleDay().equalsIgnoreCase(schedule2.getScheduleDay())) {

            // Logic for overlap: (StartA < EndB) AND (StartB < EndA)
            return schedule1.getStartTime().isBefore(schedule2.getEndTime()) &&
                    schedule2.getStartTime().isBefore(schedule1.getEndTime());
        }

        return false;
    }
}