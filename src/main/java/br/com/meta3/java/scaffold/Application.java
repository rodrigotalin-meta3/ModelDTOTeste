package br.com.meta3.java.scaffold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import br.com.meta3.java.scaffold.config.LegacyDataSourceProperties;

/**
 * Main Spring Boot application class.
 */
@SpringBootApplication
@EnableConfigurationProperties(LegacyDataSourceProperties.class) // TODO: (REVIEW) Enable binding of legacy datasource properties
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
