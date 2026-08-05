package giunei.representa_brasil.ai.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record SummarizeRequest(
		@NotBlank String tipo,
		@NotBlank String texto,
		@NotEmpty List<String> fontes
) {
}
