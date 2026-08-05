package giunei.representa_brasil.candidate.dto;

import java.util.List;

public record CandidateComplementaryData(
		ParliamentaryHistory historicoParlamentar,
		List<ExecutiveAct> atosExecutivos
) {

	public record ParliamentaryHistory(
			String origem,
			String nome,
			String partido,
			String uf,
			String perfilUrl,
			List<String> observacoes
	) {
	}

	public record ExecutiveAct(
			String tipo,
			String titulo,
			String data,
			String url
	) {
	}
}
