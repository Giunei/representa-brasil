package giunei.representa_brasil.quiz.dto;

import java.util.List;

import giunei.representa_brasil.shared.domain.Cargo;

public record QuizQuestion(
		String id,
		String escopo,
		String tema,
		String enunciado,
		List<QuizOption> opcoes,
		List<Cargo> cargosRelacionados
) {

	public record QuizOption(String id, String texto) {
	}
}
