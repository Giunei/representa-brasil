package giunei.representa_brasil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

	@Bean
	WebMvcConfigurer corsConfigurer(AppProperties properties) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
						.allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
						.allowedMethods("GET", "POST", "OPTIONS")
						.allowedHeaders("*");
			}
		};
	}
}
