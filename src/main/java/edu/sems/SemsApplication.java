package edu.sems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SemsApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SemsApplication.class);
        application.setAdditionalProfiles("dev");
        application.run(args);
    }
}
