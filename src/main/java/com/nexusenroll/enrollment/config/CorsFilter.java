package com.nexusenroll.enrollment.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

// Notice we implement both RequestFilter (for OPTIONS) and ResponseFilter (for headers)
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // 1. Intercept the browser's Preflight (OPTIONS) request
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            // Abort the request early and return a 200 OK so the browser knows it is safe
            // to proceed
            requestContext.abortWith(Response.ok().build());
        }
    }

    // 2. Attach the CORS headers to the response
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        // CRITICAL: Use putSingle() instead of add() to mathematically guarantee only
        // one value is set
        responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}