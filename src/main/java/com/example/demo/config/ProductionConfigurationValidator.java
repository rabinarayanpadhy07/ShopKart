package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ProductionConfigurationValidator {

    @Value("${app.production:false}")
    private boolean production;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${razorpay.key_id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret:}")
    private String razorpayKeySecret;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${app.security.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.security.cookie-same-site:None}")
    private String cookieSameSite;

    @Value("${spring.jpa.hibernate.ddl-auto:validate}")
    private String ddlAuto;

    @Value("${app.seed.demo-data:false}")
    private boolean seedDemoData;

    @Value("${app.seed.admin:false}")
    private boolean seedAdmin;

    @Value("${app.database.patch-enabled:false}")
    private boolean databasePatchEnabled;

    @PostConstruct
    public void validateProductionSettings() {
        if (!production) {
            return;
        }

        requireConfigured("JWT_SECRET", jwtSecret);
        requireConfigured("RAZORPAY_KEY_ID", razorpayKeyId);
        requireConfigured("RAZORPAY_KEY_SECRET", razorpayKeySecret);
        requireConfigured("GOOGLE_CLIENT_ID", googleClientId);

        if (jwtSecret.startsWith("dev-only")) {
            throw new IllegalStateException("JWT_SECRET must be replaced for production.");
        }
        if (!cookieSecure || !"None".equalsIgnoreCase(cookieSameSite)) {
            throw new IllegalStateException("Cross-site production auth cookies must use Secure and SameSite=None. Please set AUTH_COOKIE_SECURE=true and AUTH_COOKIE_SAME_SITE=None in your production environment variables.");
        }
        if ("update".equalsIgnoreCase(ddlAuto) || "create".equalsIgnoreCase(ddlAuto) || "create-drop".equalsIgnoreCase(ddlAuto)) {
            throw new IllegalStateException("DDL_AUTO must be validate or none in production.");
        }
        if (seedDemoData || seedAdmin || databasePatchEnabled) {
            throw new IllegalStateException("Seeders and database patch runners must be disabled in production.");
        }
    }

    private void requireConfigured(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " must be configured for production.");
        }
    }
}
