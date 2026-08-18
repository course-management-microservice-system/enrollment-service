package com.nexusenroll.enrollment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Singleton
public class AuthServiceClient {

    private final HttpClient httpClient;
    private final String authServiceBaseUrl = "http://localhost:8000/api/auth"; // Adjust port if needed

    public AuthServiceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Fetches a student's internal userId by their email.
     * Returns null if the user is not found.
     */
    public String getUserIdByEmail(String email) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authServiceBaseUrl + "/search?email=" + email))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            // Assumes Auth service returns a JSON object with a "userId" field
            return root.has("userId") ? root.get("userId").asText() : null;
        }
        return null;
    }
}