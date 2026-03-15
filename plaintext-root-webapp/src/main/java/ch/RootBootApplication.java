package ch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"ch.plaintext"})
@EntityScan(basePackages = {"ch.plaintext"})
@EnableJpaRepositories(basePackages = {"ch.plaintext"})
@EnableScheduling
@EnableAsync
public class RootBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(RootBootApplication.class, args);
    }
}
