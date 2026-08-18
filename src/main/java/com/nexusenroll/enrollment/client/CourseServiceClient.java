package com.nexusenroll.enrollment.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class CourseServiceClient {

    private final HttpClient httpClient;
    private final String courseServiceBaseUrl = "http://localhost:8000/api/courses"; // Adjust port as needed

    public CourseServiceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Attempts to reserve a seat.
     * Returns 200 OK if successful, 409 Conflict if full.
     */
    public int reserveSeat(String courseId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(courseServiceBaseUrl + "/" + courseId + "/reserve-seat"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    /**
     * The Compensating Transaction (Rollback)
     * Used to release a seat if our Enrollment DB crashes after reserving.
     */
    public void releaseSeat(String courseId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(courseServiceBaseUrl + "/" + courseId + "/release-seat"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            // Log this severely. In a production system, this would go to a Dead Letter
            // Queue.
            System.err.println("CRITICAL: Failed to release seat for rollback on course " + courseId);
        }
    }

    /**
     * Fetches prerequisite course IDs from the Course Service.
     */
    public List<String> getPrerequisites(String courseId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(courseServiceBaseUrl + "/" + courseId + "/prerequisites"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // TODO: Parse the JSON response body into a List<String> of
        // prerequisiteCourseIds
        // Example: ["CS101", "MATH201"]
        return parseJsonToList(response.body());
    }

    /**
     * Parses the JSON response from the Course Service and extracts a list of
     * prerequisite course IDs.
     */
    private List<String> parseJsonToList(String jsonBody) throws Exception {
        if (jsonBody == null || jsonBody.trim().isEmpty()) {
            return List.of(); // Return empty list if no prerequisites exist
        }

        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Parse the JSON array of objects into a List of Maps
        List<Map<String, Object>> prerequisiteObjects = objectMapper.readValue(
                jsonBody,
                new TypeReference<List<Map<String, Object>>>() {
                });

        // 2. Extract the "prerequisiteCourseId" field from each object using Java
        // Streams
        return prerequisiteObjects.stream()
                .map(obj -> (String) obj.get("prerequisiteCourseId"))
                .collect(Collectors.toList());
    }

    /**
     * Fetches the schedule (days and times) for a specific course from the Course
     * Service.
     */
    public CourseScheduleDTO getCourseSchedule(String courseId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(courseServiceBaseUrl + "/" + courseId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            // Register JavaTimeModule to handle parsing JSON strings into LocalTime objects
            mapper.registerModule(new JavaTimeModule());

            return mapper.readValue(response.body(), CourseScheduleDTO.class);
        }

        return null;
    }

    /**
     * Fetches a course's internal courseId by its courseCode.
     * Returns null if the course is not found.
     */
    public String getCourseIdByCode(String courseCode) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(courseServiceBaseUrl + "/code/" + courseCode))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            // Assumes Course service returns a JSON object with a "courseId" field
            return root.has("courseId") ? root.get("courseId").asText() : null;
        }
        return null;
    }
}