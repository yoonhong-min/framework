package com.example.framework.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    @Getter
    @Setter
    private String baseDir;

    @Getter
    @Setter
    private String publicUrlPrefix = "/files";

    @Getter
    @Setter
    private List<String> allowedContentTypes;

}