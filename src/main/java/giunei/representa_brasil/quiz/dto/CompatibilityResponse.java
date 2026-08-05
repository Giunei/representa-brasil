package giunei.representa_brasil.quiz.dto;

import java.util.List;

import giunei.representa_brasil.shared.domain.Cargo;

public record CompatibilityResponse(
		List<CargoCompatibility> porCargo,
		List<String> limitacoes,
		String explicacao
) {

	public record CargoCompatibility(
			Cargo cargo,
			List<MatchPreview> aproximacoes
	) {
	}

	public record MatchPreview(
			String candidatoId,
			String nome,
			String partido,
			int afinidadePercentual,
			String motivo
	) {
	}
}
