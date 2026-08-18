package com.nexusenroll.enrollment.db;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

import com.nexusenroll.enrollment.db.entities.EnrollmentEntity;
import com.nexusenroll.enrollment.db.entities.EnrollmentStatus;
import com.nexusenroll.enrollment.db.entities.WaitlistEntity;
import com.nexusenroll.enrollment.db.entities.WaitlistStatus;
import com.nexusenroll.enrollment.db.entities.AdminAuditLogEntity;

@Singleton
public class EnrollmentRepository {

    @Inject
    private EntityManager em;

    /**
     * Saves a new enrolment record (e.g., when a student successfully registers).
     */
    public void saveEnrollment(EnrollmentEntity enrollment) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (enrollment.getEnrolledAt() == null) {
                enrollment.setEnrolledAt(LocalDateTime.now());
            }
            em.persist(enrollment);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive())
                tx.rollback();
            throw e;
        }
    }

    /**
     * Drops a course by updating the enrolment status to DROPPED.
     */
    public EnrollmentEntity dropCourse(String studentId, String courseId) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            TypedQuery<EnrollmentEntity> query = em.createQuery(
                    "SELECT e FROM EnrollmentEntity e WHERE e.studentId = :studentId AND e.courseId = :courseId AND e.status = :status",
                    EnrollmentEntity.class);
            query.setParameter("studentId", studentId);
            query.setParameter("courseId", courseId);
            query.setParameter("status", EnrollmentStatus.ENROLLED);

            List<EnrollmentEntity> results = query.getResultList();

            if (results.isEmpty()) {
                tx.rollback();
                return null;
            }

            EnrollmentEntity enrollment = results.get(0);
            enrollment.setStatus(EnrollmentStatus.DROPPED);
            enrollment.setDroppedAt(LocalDateTime.now());

            // JPA automatically syncs managed entities on commit
            tx.commit();
            return enrollment;

        } catch (Exception e) {
            if (tx.isActive())
                tx.rollback();
            throw e;
        }
    }

    /**
     * Fetches the active class roster for a faculty member.
     */
    public List<EnrollmentEntity> findActiveRosterByCourse(String courseId) {
        return em.createQuery(
                "SELECT e FROM EnrollmentEntity e WHERE e.courseId = :courseId AND e.status = :status",
                EnrollmentEntity.class)
                .setParameter("courseId", courseId)
                .setParameter("status", EnrollmentStatus.ENROLLED)
                .getResultList();
    }

    /**
     * Fetches a student's current active schedule.
     */
    public List<EnrollmentEntity> findActiveScheduleByStudent(String studentId) {
        return em.createQuery(
                "SELECT e FROM EnrollmentEntity e WHERE e.studentId = :studentId AND e.status = :status",
                EnrollmentEntity.class)
                .setParameter("studentId", studentId)
                .setParameter("status", EnrollmentStatus.ENROLLED)
                .getResultList();
    }

    /**
     * Adds a student to the waitlist when a course is full.
     */
    public void saveWaitlist(WaitlistEntity waitlistEntry) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (waitlistEntry.getCreatedAt() == null) {
                waitlistEntry.setCreatedAt(LocalDateTime.now());
            }
            em.persist(waitlistEntry);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive())
                tx.rollback();
            throw e;
        }
    }

    /**
     * Saves an admin audit log (used when an admin forces an enrolment override).
     */
    public void saveAdminAuditLog(AdminAuditLogEntity auditLog) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (auditLog.getPerformedAt() == null) {
                auditLog.setPerformedAt(LocalDateTime.now());
            }
            em.persist(auditLog);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive())
                tx.rollback();
            throw e;
        }
    }

    public void updateEnrollment(EnrollmentEntity enrollment) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // merge() updates the existing record in the DB with the new state
            em.merge(enrollment);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Gets the current number of active students on the waitlist for a specific
     * course.
     */
    public int getWaitlistCountForCourse(String courseId) {
        Long count = em.createQuery(
                "SELECT COUNT(w) FROM WaitlistEntity w WHERE w.courseId = :courseId AND w.status = :status",
                Long.class)
                .setParameter("courseId", courseId)
                .setParameter("status", WaitlistStatus.WAITING)
                .getSingleResult();

        // COUNT returns a Long, so we convert it safely to an int
        return count != null ? count.intValue() : 0;
    }

    /**
     * Checks if a student has an APPROVED passing grade for a specific course.
     */
    public boolean hasPassedCourse(String studentId, String courseId) {
        Long count = em.createQuery(
                "SELECT COUNT(g) FROM GradeSubmissionEntity g " +
                        "WHERE g.studentId = :studentId AND g.courseId = :courseId " +
                        "AND g.status = 'APPROVED' AND g.letterGrade NOT IN ('F', 'D')",
                Long.class)
                .setParameter("studentId", studentId)
                .setParameter("courseId", courseId)
                .getSingleResult();

        return count != null && count > 0;
    }

    /**
     * Checks if a student is already enrolled or has a pending request for a
     * specific course.
     */
    public boolean isStudentAlreadyEnrolled(String studentId, String courseId) {
        Long count = em.createQuery(
                "SELECT COUNT(e) FROM EnrollmentEntity e " +
                        "WHERE e.studentId = :studentId AND e.courseId = :courseId " +
                        "AND e.status IN (:enrolledStatus, :pendingStatus)",
                Long.class)
                .setParameter("studentId", studentId)
                .setParameter("courseId", courseId)
                .setParameter("enrolledStatus", EnrollmentStatus.ENROLLED)
                .setParameter("pendingStatus", EnrollmentStatus.PENDING)
                .getSingleResult();

        return count != null && count > 0;
    }
}