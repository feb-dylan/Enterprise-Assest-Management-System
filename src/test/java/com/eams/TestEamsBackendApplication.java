package com.eams;

import org.springframework.boot.SpringApplication;

/**
 * Runs the real backend WITHOUT MySQL: in-memory H2 database + demo data.
 * Handy for trying the API, Swagger UI, or recording a demo.
 *
 *   ./mvnw spring-boot:test-run
 *
 * Then open http://localhost:8080/swagger-ui.html
 * and log in as admin@eams.demo / Password123! (all data is lost on stop).
 */
public class TestEamsBackendApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EamsBackendApplication.class);
        app.setAdditionalProfiles("test");
        System.setProperty("app.seed.demo-data", "true");
        System.setProperty("app.upload-dir", "target/dev-uploads");
        app.run(args);
    }
}
