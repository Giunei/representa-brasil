package giunei.representa_brasil.quiz;

import java.util.List;

import org.springframework.stereotype.Service;

import giunei.representa_brasil.quiz.dto.CompatibilityRequest;
import giunei.representa_brasil.quiz.dto.CompatibilityResponse;
import giunei.representa_brasil.quiz.dto.QuizQuestion;
import giunei.representa_brasil.shared.domain.Cargo;

@Service
public class QuizService {

	public List<QuizQuestion> listQuestions(String escopo) {
		List<QuizQuestion> questions = List.of(
				new QuizQuestion(
						"n1",
						"NACIONAL",
						"Economia",
						"Em temas econômicos nacionais, o que você considera mais prioritário?",
						List.of(
								new QuizQuestion.QuizOption("a", "Controle da inflação"),
								new QuizQuestion.QuizOption("b", "Geração de emprego"),
								new QuizQuestion.QuizOption("c", "Investimento em infraestrutura")),
						List.of(Cargo.PRESIDENTE, Cargo.DEPUTADO_FEDERAL, Cargo.SENADOR)),
				new QuizQuestion(
						"e1",
						"ESTADUAL",
						"Saúde",
						"No seu estado, qual prioridade de saúde pública faz mais sentido para você?",
						List.of(
								new QuizQuestion.QuizOption("a", "Atenção básica"),
								new QuizQuestion.QuizOption("b", "Hospitais e urgência"),
								new QuizQuestion.QuizOption("c", "Prevenção e vigilância")),
						List.of(Cargo.GOVERNADOR, Cargo.DEPUTADO_ESTADUAL)));

		if (escopo == null || escopo.isBlank()) {
			return questions;
		}

		String filter = escopo.trim().toUpperCase();
		return questions.stream()
				.filter(q -> q.escopo().equalsIgnoreCase(filter))
				.toList();
	}

	public CompatibilityResponse compatibility(CompatibilityRequest request) {
		List<CompatibilityResponse.CargoCompatibility> porCargo = request.cargos().stream()
				.map(cargo -> new CompatibilityResponse.CargoCompatibility(cargo, List.of()))
				.toList();

		return new CompatibilityResponse(
				porCargo,
				List.of(
						"O quiz ainda não calcula afinidade real com candidatos.",
						"Nenhuma recomendação de voto é emitida.",
						"Quando ativo, o resultado mostrará aproximação temática com base em evidências públicas."),
				"Contrato pronto: as respostas foram recebidas e serão usadas para calcular compatibilidade por cargo em etapa posterior.");
	}
}
