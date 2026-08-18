package com.nexusenroll.enrollment;

import java.util.Optional;
import java.util.Set;

import com.nexusenroll.enrollment.api.EnrollmentResource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
public class EnrollmentApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(EnrollmentResource.class);
    }

    public static void main(String[] args) throws InterruptedException {
        int port = Optional.ofNullable(System.getenv("PORT"))
                .map(Integer::valueOf)
                .orElse(8082);

        SeBootstrap.Configuration config = SeBootstrap.Configuration.builder()
                .property(SeBootstrap.Configuration.PORT, port)
                .build();

        SeBootstrap.start(new EnrollmentApplication(), config).thenAccept(instance -> {
            System.out.println("Course Service running on http://localhost:" + port + "/api");
        });
        Thread.currentThread().join();
    }
}