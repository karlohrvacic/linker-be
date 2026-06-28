package cc.hrva.ladica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LadicaSyncApplication {
    public static void main(final String[] args) {
        SpringApplication.run(LadicaSyncApplication.class, args);
    }
}
