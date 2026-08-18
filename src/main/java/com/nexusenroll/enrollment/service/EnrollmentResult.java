package com.nexusenroll.enrollment.service;

public class EnrollmentResult {
    private boolean success;
    private String message;
    private int httpStatus;

    public EnrollmentResult(boolean success, String message, int httpStatus) {
        this.success = success;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}