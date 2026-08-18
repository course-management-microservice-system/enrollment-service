package com.nexusenroll.enrollment.config;

import com.nexusenroll.enrollment.logic.JwtUtility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Secured // This binds the filter only to endpoints annotated with @Secured
public class AuthenticationFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // 1. Extract the Authorization header from the incoming request
        String authHeader = requestContext.getHeaderString("Authorization");

        // 2. Reject if the header is missing or doesn't start with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Missing or invalid Authorization header\"}")
                    .build());
            return;
        }

        // 3. Isolate the actual JWT string
        String token = authHeader.substring(7);

        try {
            // 4. Mathematically verify the token's signature and expiration.
            // If the token was tampered with, or if it is expired, this throws an
            // Exception.
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(JwtUtility.getSecretKey()) // Must exactly match the key used in auth-service
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 5. Inject the validated user details into the request context
            // so your business logic (like CourseResource) can access who is making the
            // request.
            requestContext.setProperty("username", claims.getSubject());
            requestContext.setProperty("role", claims.get("role"));

        } catch (Exception e) {
            // 6. Abort the request before it ever reaches your core business logic
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid or Expired JWT Token\"}")
                    .build());
        }
    }
}