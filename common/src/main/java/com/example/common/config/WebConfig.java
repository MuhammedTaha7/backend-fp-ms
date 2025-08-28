package com.example.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files from uploads directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Alternative: if you want to serve from a specific path
        // registry.addResourceHandler("/uploads/**")
        //         .addResourceLocations("file:C:/path/to/your/uploads/")
        //         .setCachePeriod(3600);
    }
}