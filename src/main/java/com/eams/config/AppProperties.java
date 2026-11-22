package com.eams.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed access to all "app.*" settings in application.properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String uploadDir,
        String frontendUrl,
        String backendUrl,
        Mail mail,
        Cors cors,
        Auth auth,
        Ai ai,
        Seed seed
) {
    public record Mail(boolean enabled, String from) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Auth(
            boolean publicRegistration,
            boolean requireEmailVerification,
            int maxLoginAttempts,
            int lockoutMinutes) {}

    /**
     * AI via OpenRouter (OpenAI-compatible API).
     *
     * @param model       model used by "Ask EAMS" (must support tool calling)
     * @param visionModel model used for damage-photo triage (must accept images);
     *                    falls back to {@code model} when empty
     */
    public record Ai(boolean enabled, String apiKey, String baseUrl,
                     String model, String visionModel) {

        public boolean isConfigured() {
            return enabled && notBlank(apiKey) && notBlank(model);
        }

        public String effectiveVisionModel() {
            return notBlank(visionModel) ? visionModel : model;
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    public record Seed(boolean demoData, String demoPassword) {}
}
