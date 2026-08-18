package com.nexusenroll.enrollment.config;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

public class WebFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        context.register(AuthenticationFilter.class, CorsFilter.class);
        return true;
    }
}