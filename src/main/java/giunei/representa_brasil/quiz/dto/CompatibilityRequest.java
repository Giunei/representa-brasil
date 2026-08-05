package giunei.representa_brasil.quiz.dto;

import java.util.List;

import giunei.representa_brasil.shared.domain.Cargo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CompatibilityRequest(
		@NotEmpty List<Cargo> cargos,
		@NotEmpty List<Answer> respostas
) {

	public record Answer(
			@NotNull String perguntaId,
			@NotNull String opcaoId
	) {
	}
}
