package com.nexusenroll.enrollment.client;

import java.time.LocalTime;

public class CourseScheduleDTO {
    private String scheduleDay;
    private LocalTime startTime;
    private LocalTime endTime;

    // Getters and Setters
    public String getScheduleDay() {
        return scheduleDay;
    }

    public void setScheduleDay(String scheduleDay) {
        this.scheduleDay = scheduleDay;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}