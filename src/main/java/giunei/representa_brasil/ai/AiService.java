package giunei.representa_brasil.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import giunei.representa_brasil.ai.dto.SummarizeRequest;
import giunei.representa_brasil.ai.dto.SummarizeResponse;

@Service
public class AiService {

	private static final List<String> BASE_LIMITATIONS = List.of(
			"O conteúdo é informativo e não recomenda candidatos.",
			"Resumos por IA podem conter imprecisões; consulte sempre as fontes oficiais.",
			"A análise se limita aos documentos fornecidos na requisição.");

	private final AiClient aiClient;

	public AiService(AiClient aiClient) {
		this.aiClient = aiClient;
	}

	public SummarizeResponse summarize(SummarizeRequest request) {
		List<String> limitacoes = new ArrayList<>(BASE_LIMITATIONS);
		String tipo = request.tipo().trim().toLowerCase(Locale.ROOT);

		if (!aiClient.isEnabled()) {
			limitacoes.add("Integração com Ollama desabilitada. Retornando recorte textual sem geração.");
			String fallback = buildFallback(tipo, request.texto());
			return new SummarizeResponse(fallback, false, request.fontes(), limitacoes);
		}

		String generated = aiClient.summarize(tipo, request.texto(), request.fontes());
		if (generated == null || generated.isBlank()) {
			limitacoes.add("Modelo de IA indisponível. Retornando recorte textual sem geração.");
			return new SummarizeResponse(buildFallback(tipo, request.texto()), false, request.fontes(), limitacoes);
		}

		return new SummarizeResponse(generated, true, request.fontes(), limitacoes);
	}

	private static String buildFallback(String tipo, String texto) {
		String cleaned = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
		if (cleaned.length() > 400) {
			cleaned = cleaned.substring(0, 400) + "...";
		}
		return "Recorte do documento (" + tipo + "): " + cleaned;
	}
}
