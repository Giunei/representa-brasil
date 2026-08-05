package giunei.representa_brasil.quiz;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import giunei.representa_brasil.quiz.dto.CompatibilityRequest;
import giunei.representa_brasil.quiz.dto.CompatibilityResponse;
import giunei.representa_brasil.quiz.dto.QuizQuestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/quiz")
@Tag(name = "Quiz")
@Validated
public class QuizController {

	private final QuizService quizService;

	public QuizController(QuizService quizService) {
		this.quizService = quizService;
	}

	@GetMapping("/questions")
	@Operation(summary = "Listar perguntas do quiz")
	public List<QuizQuestion> questions(@RequestParam(required = false) String escopo) {
		return quizService.listQuestions(escopo);
	}

	@PostMapping("/compatibility")
	@Operation(summary = "Calcular compatibilidade (stub neutro)")
	public CompatibilityResponse compatibility(@Valid @RequestBody CompatibilityRequest request) {
		return quizService.compatibility(request);
	}
}
