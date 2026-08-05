package giunei.representa_brasil.search.dto;

import java.util.List;

import giunei.representa_brasil.bill.dto.BillSearchItem;
import giunei.representa_brasil.candidate.dto.CandidateSearchItem;

public record SearchResponse(
		String consulta,
		List<CandidateSearchItem> candidatos,
		List<BillSearchItem> projetos,
		List<String> observacoes
) {
}
