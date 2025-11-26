package com.example.file.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "file.storage")
@Getter
@Setter
public class FileStorageProperties {

    /**
     * 로컬 파일 저장 루트 경로.
     */
    private String rootPath = "build/storage/files";

}
