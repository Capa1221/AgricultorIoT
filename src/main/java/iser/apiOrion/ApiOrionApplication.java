package iser.apiOrion;

import iser.apiOrion.auth.serviceImpl.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling; // ¡Importa esta anotación!

@SpringBootApplication(scanBasePackages = "iser.apiOrion")
@EnableMongoRepositories
@EnableScheduling // <-- ¡Añade esta línea aquí!
public class ApiOrionApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ApiOrionApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ApiOrionApplication.class);
    }
}

