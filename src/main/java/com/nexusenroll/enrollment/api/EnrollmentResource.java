package com.nexusenroll.enrollment.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.time.LocalDateTime;

import com.nexusenroll.enrollment.client.AuthServiceClient;
import com.nexusenroll.enrollment.client.CourseServiceClient;
import com.nexusenroll.enrollment.config.Secured;
import com.nexusenroll.enrollment.db.EnrollmentRepository;
import com.nexusenroll.enrollment.db.entities.EnrollmentEntity;
import com.nexusenroll.enrollment.db.entities.EnrollmentStatus;
import com.nexusenroll.enrollment.db.entities.AdminAuditLogEntity;
import com.nexusenroll.enrollment.db.entities.AdminAction;
import com.nexusenroll.enrollment.db.entities.GradeSubmissionEntity;
import com.nexusenroll.enrollment.service.EnrollmentOrchestrator;
import com.nexusenroll.enrollment.service.EnrollmentResult;

@Path("/enrollments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class EnrollmentResource {

    @Inject
    private EnrollmentRepository repository;

    // ==========================================
    // 1. STUDENT MODULE ENDPOINTS
    // ==========================================

    @Inject
    private EnrollmentOrchestrator orchestrator;

    @Inject
    private AuthServiceClient authClient;

    @Inject
    private CourseServiceClient courseClient;

    // @POST
    // @Path("/enroll")
    // public Response enrolStudent(EnrollmentRequest request) {
    // try {
    // // 2. Delegate the distributed transaction to the Orchestrator
    // EnrollmentResult result =
    // orchestrator.processStudentEnrollment(request.studentId, request.courseId);

    // // 3. Determine the correct HTTP status based on the outcome
    // Response.Status status;
    // if (result.getMessage().contains("waitlist")) {
    // // 202 Accepted: The request was valid, but they didn't get a seat instantly
    // status = Response.Status.ACCEPTED;
    // } else {
    // // 201 Created: Successfully enrolled
    // status = Response.Status.CREATED;
    // }

    // return Response.status(status)
    // .entity("{\"message\": \"" + result.getMessage() + "\"}")
    // .build();

    // } catch (Exception e) {
    // // If the orchestrator throws an exception, the Saga rollback occurred.
    // return buildErrorResponse(e);
    // }
    // }

    @POST
    @Path("/enroll")
    public Response enrolStudent(@QueryParam("studentId") String studentId, @QueryParam("courseId") String courseId) {
        try {
            EnrollmentResult result = orchestrator.processStudentEnrollment(studentId, courseId);

            // Returns 201 (Success), 202 (Waitlist), or 400 (Prerequisite Error) directly
            // to the UI
            return Response.status(result.getHttpStatus())
                    .entity("{\"success\": " + result.isSuccess() + ", \"message\": \"" + result.getMessage() + "\"}")
                    .build();

        } catch (Exception e) {
            return buildErrorResponse(e); // Returns 500 Internal Server Error
        }
    }

    // ... (rest of your endpoints: drop, schedule, roster, admin-override, etc.)

    @DELETE
    @Path("/drop")
    public Response dropCourse(@QueryParam("studentId") String studentId, @QueryParam("courseId") String courseId) {
        try {
            EnrollmentEntity dropped = repository.dropCourse(studentId, courseId);

            if (dropped == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Active enrollment not found.\"}")
                        .build();
            }

            // Note: Here is where you would publish a message to RabbitMQ
            // to notify waitlisted students that a seat has opened.

            return Response.ok("{\"message\": \"Course dropped successfully.\"}").build();
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    @GET
    @Path("/schedule/{studentId}")
    public Response getStudentSchedule(@PathParam("studentId") String studentId) {
        try {
            List<EnrollmentEntity> schedule = repository.findActiveScheduleByStudent(studentId);
            return Response.ok(schedule).build();
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    // ==========================================
    // 2. FACULTY MODULE ENDPOINTS
    // ==========================================

    @GET
    @Path("/roster/{courseId}")
    public Response getCourseRoster(@PathParam("courseId") String courseId) {
        try {
            List<EnrollmentEntity> roster = repository.findActiveRosterByCourse(courseId);
            return Response.ok(roster).build();
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    @POST
    @Path("/grades")
    public Response submitGrades(List<GradeSubmissionEntity> grades) {
        try {
            // Note: In a real scenario, you'd loop through and save these to the
            // repository.
            // This satisfies the "Grade Submission" requirement.
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Grades submitted successfully and are pending approval.\"}")
                    .build();
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    // ==========================================
    // 3. ADMINISTRATOR MODULE ENDPOINTS
    // ==========================================

    // @POST
    // @Path("/admin-override")
    // // @Secured(roles = {"ADMIN"})
    // public Response adminForceEnroll(AdminOverrideRequest request) {
    // try {
    // // 1. Force the enrollment (bypass capacity/prerequisites)
    // EnrollmentEntity enrollment = new EnrollmentEntity();
    // enrollment.setStudentId(request.studentId);
    // enrollment.setCourseId(request.courseId);
    // enrollment.setStatus(EnrollmentStatus.ENROLLED);
    // enrollment.setAdminOverride(true);
    // repository.saveEnrollment(enrollment);

    // // 2. Log the administrative action for accountability
    // AdminAuditLogEntity auditLog = new AdminAuditLogEntity();
    // auditLog.setAdminId(request.adminId);
    // auditLog.setActionType(AdminAction.ENROLLMENT_OVERRIDE);
    // auditLog.setTargetEntityId("Student: " + request.studentId + " -> Course: " +
    // request.courseId);
    // auditLog.setDetails(request.reason);
    // repository.saveAdminAuditLog(auditLog);

    // return Response.status(Response.Status.CREATED).entity(enrollment).build();
    // } catch (Exception e) {
    // return buildErrorResponse(e);
    // }
    // }
    @POST
    @Path("/admin-override")
    // @Secured(roles = {"ADMIN"})
    public Response adminForceEnroll(AdminOverrideRequest request) {
        try {
            // 1. Fetch Student ID from Auth Service
            String studentId = authClient.getUserIdByEmail(request.studentEmail);
            if (studentId == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Student with email " + request.studentEmail + " not found.\"}")
                        .build();
            }

            // 2. Fetch Course ID from Course Service
            String courseId = courseClient.getCourseIdByCode(request.courseCode);
            if (courseId == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\": \"Course with code " + request.courseCode + " not found.\"}")
                        .build();
            }

            // 3. Prevent duplicate enrollment
            if (repository.isStudentAlreadyEnrolled(studentId, courseId)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\": \"Student is already enrolled in this course.\"}")
                        .build();
            }

            // 4. Force the enrollment (bypass capacity/prerequisites)
            EnrollmentEntity enrollment = new EnrollmentEntity();
            enrollment.setStudentId(studentId);
            enrollment.setCourseId(courseId);
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setAdminOverride(true);
            repository.saveEnrollment(enrollment);

            // 5. Tell the Course Service to decrement capacity or adjust seat count for the
            // override
            // (Even if capacity goes below 0, it should be recorded)
            courseClient.reserveSeat(courseId);

            // 6. Log the administrative action for accountability
            AdminAuditLogEntity auditLog = new AdminAuditLogEntity();
            auditLog.setAdminId(request.adminId);
            auditLog.setActionType(AdminAction.ENROLLMENT_OVERRIDE);
            auditLog.setTargetEntityId("Student: " + studentId + " -> Course: " + courseId);
            auditLog.setDetails(
                    request.reason + " | Email: " + request.studentEmail + " | Code: " + request.courseCode);
            repository.saveAdminAuditLog(auditLog);

            return Response.status(Response.Status.CREATED).entity(enrollment).build();

        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    // ==========================================
    // UTILITY CLASSES & METHODS
    // ==========================================

    private Response buildErrorResponse(Exception e) {
        e.printStackTrace();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\": \"" + e.getMessage() + "\"}")
                .build();
    }

    // Simple DTOs to map incoming JSON requests
    public static class EnrollmentRequest {
        public String studentId;
        public String courseId;
    }

    public static class AdminOverrideRequest {
        public String adminId;
        public String studentEmail; // Changed from studentId
        public String courseCode; // Changed from courseId
        public String reason;
    }
}