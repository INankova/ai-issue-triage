package io.github.inankova.triage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // binds @ConfigurationProperties records such as GitHubProperties
public class TriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriageApplication.class, args);
    }
}
