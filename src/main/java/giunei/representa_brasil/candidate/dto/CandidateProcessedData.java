package giunei.representa_brasil.candidate.dto;

import java.util.List;

public record CandidateProcessedData(
		List<TimelineEvent> linhaDoTempo,
		CandidateStats estatisticas,
		String resumoAtuacao
) {

	public record TimelineEvent(String data, String titulo, String descricao, String tipo) {
	}

	public record CandidateStats(
			Integer totalBensDeclarados,
			Integer totalDocumentos,
			Boolean possuiPlanoGoverno,
			Boolean possuiHistoricoParlamentar
	) {
	}
}
