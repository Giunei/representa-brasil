package giunei.representa_brasil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Representa Brasil API")
						.description("Consulta pública e neutra de candidatos, projetos e fontes oficiais.")
						.version("0.1.0"));
	}
}
