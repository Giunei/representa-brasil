package giunei.representa_brasil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RepresentaBrasilApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepresentaBrasilApplication.class, args);
	}
}
