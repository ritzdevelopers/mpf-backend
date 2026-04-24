package com.mypropertyfact.estate.configs;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

/**
 * Large Excel/zip project uploads (e.g. {@code /api/v1/excel-upload/projects}).
 * Mirrors the limits in application configuration so the embedded server and multipart
 * parser stay consistent at runtime.
 */
@Configuration
public class LargeHttpUploadConfig {

    private static final DataSize MAX_UPLOAD = DataSize.ofMegabytes(300);

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMaxHttpPostSize() {
        return factory -> factory.addConnectorCustomizers(connector ->
                connector.setMaxPostSize((int) MAX_UPLOAD.toBytes()));
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_UPLOAD);
        factory.setMaxRequestSize(MAX_UPLOAD);
        return factory.createMultipartConfig();
    }
}
