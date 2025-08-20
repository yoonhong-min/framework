package com.example.framework.config;

import com.example.framework.file.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties props;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Path.of(props.getBaseDir()).toAbsolutePath().normalize();
        // 예: /files/** -> file:/C:/Users/계정/framework-uploads/
        registry.addResourceHandler(props.getPublicUrlPrefix() + "/**")
                .addResourceLocations(base.toUri().toString())
                .setCachePeriod(3600); // 1시간 캐시 (원하는 대로)
    }
}
