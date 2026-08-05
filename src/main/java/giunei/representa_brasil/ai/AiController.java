package giunei.representa_brasil.ai;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import giunei.representa_brasil.ai.dto.SummarizeRequest;
import giunei.representa_brasil.ai.dto.SummarizeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "IA")
@Validated
public class AiController {

	private final AiService aiService;

	public AiController(AiService aiService) {
		this.aiService = aiService;
	}

	@PostMapping("/summarize")
	@Operation(summary = "Resumir texto com regras de neutralidade")
	public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest request) {
		return aiService.summarize(request);
	}
}
