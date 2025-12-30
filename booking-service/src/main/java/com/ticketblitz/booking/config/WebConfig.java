package com.ticketblitz.booking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired // Inject the Spring-managed bean
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register the INJECTED bean, do NOT use 'new'
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/bookings/**");
    }
}
