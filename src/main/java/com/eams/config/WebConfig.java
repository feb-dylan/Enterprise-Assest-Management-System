package com.eams.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        /*
         * Upload code saves to: "uploads/assets/<file>"
         * (relative to the working directory Spring Boot runs from)
         *
         * So the file at runtime is:
         *   <working-dir>/uploads/assets/<file>
         *
         * We register:
         *   URL  /uploads/**        →  <working-dir>/uploads/
         *
         * Then:
         *   GET /uploads/assets/x.png
         *   maps to
         *   <working-dir>/uploads/assets/x.png  ✅
         */

        String uploadDir = Paths
                .get("uploads")
                .toAbsolutePath()
                .normalize()
                .toString();

        System.out.println("Serving /uploads/** from: " + uploadDir);

        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}