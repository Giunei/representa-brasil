package giunei.representa_brasil.ai.dto;

import java.util.List;

public record SummarizeResponse(
		String resumo,
		boolean geradoPorIa,
		List<String> fontes,
		List<String> limitacoes
) {
}
