package com.es.transalte.skytranslate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        // For CSS files
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        // For font files
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/");
    }
}