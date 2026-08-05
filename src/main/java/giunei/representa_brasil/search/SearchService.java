package giunei.representa_brasil.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import giunei.representa_brasil.bill.BillService;
import giunei.representa_brasil.bill.dto.BillSearchItem;
import giunei.representa_brasil.candidate.CandidateService;
import giunei.representa_brasil.candidate.dto.CandidateSearchItem;
import giunei.representa_brasil.search.dto.SearchResponse;
import giunei.representa_brasil.shared.domain.Cargo;
import giunei.representa_brasil.shared.exception.UpstreamUnavailableException;

@Service
public class SearchService {

	private final CandidateService candidateService;
	private final BillService billService;

	public SearchService(CandidateService candidateService, BillService billService) {
		this.candidateService = candidateService;
		this.billService = billService;
	}

	public SearchResponse search(String q, Cargo cargo, String uf, String partido) {
		List<String> observacoes = new ArrayList<>();
		observacoes.add("Busca baseada em filtros. Interpretação por IA será adicionada em etapa posterior.");
		observacoes.add("Resultados são informativos e não constituem recomendação de voto.");

		List<CandidateSearchItem> candidatos = List.of();
		if (cargo != null) {
			try {
				candidatos = candidateService.search(q, cargo, uf, partido);
			}
			catch (UpstreamUnavailableException | IllegalArgumentException ex) {
				observacoes.add("Busca de candidatos indisponível: " + ex.getMessage());
			}
		}
		else {
			observacoes.add("Informe o parâmetro 'cargo' para incluir candidatos nos resultados.");
		}

		List<BillSearchItem> projetos;
		try {
			projetos = billService.search(q, null, null, null);
		}
		catch (UpstreamUnavailableException ex) {
			projetos = List.of();
			observacoes.add("Busca de projetos indisponível no momento.");
		}

		return new SearchResponse(q, candidatos, projetos, observacoes);
	}
}
