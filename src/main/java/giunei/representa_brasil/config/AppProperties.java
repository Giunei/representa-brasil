package giunei.representa_brasil.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "representa")
public record AppProperties(
		Cors cors,
		Http http,
		Election election,
		Integration integration,
		Ai ai
) {

	public record Cors(List<String> allowedOrigins) {
	}

	public record Http(Duration connectTimeout, Duration readTimeout, String userAgent) {
	}

	public record Election(int year, long id) {
	}

	public record Integration(Tse tse, Camara camara, Senado senado, Planalto planalto) {
	}

	public record Tse(String baseUrl) {
	}

	public record Camara(String baseUrl) {
	}

	public record Senado(String baseUrl) {
	}

	public record Planalto(boolean enabled) {
	}

	public record Ai(boolean enabled, String baseUrl, String model, Duration timeout) {
	}
}
