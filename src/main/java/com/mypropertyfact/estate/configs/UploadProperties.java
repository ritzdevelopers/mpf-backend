package com.mypropertyfact.estate.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {
    private String dir;
    private HomeBannerConfig homeBannerConfig;

    @Data
    public static class HomeBannerConfig {
        private String folderName;
        private int desktopWidth;
        private int desktopHeight;
        private int mobileWidth;
        private int mobileHeight;
        private int tabletWidth;
        private int tabletHeight;
        private float defaultQuality;
    }
}
