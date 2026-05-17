package org.isihop.fr.perTranslateService;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PerTranslateServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PerTranslateServiceApplication.class);
        application.setBannerMode(Mode.OFF);
        application.run(args);
    }
}