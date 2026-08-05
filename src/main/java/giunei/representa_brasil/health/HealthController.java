package giunei.representa_brasil.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

	@GetMapping
	@Operation(summary = "Verifica se a API está no ar")
	public Map<String, String> health() {
		return Map.of(
				"status", "UP",
				"service", "representa-brasil");
	}
}
