package sn.jappo.jappo_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/livrables}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String cheminAbsolu = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/fichiers/**")
                .addResourceLocations("file:" + cheminAbsolu + "/");
    }
}