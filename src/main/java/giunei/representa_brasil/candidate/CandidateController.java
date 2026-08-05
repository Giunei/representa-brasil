package giunei.representa_brasil.candidate;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import giunei.representa_brasil.candidate.dto.CandidateComplementaryData;
import giunei.representa_brasil.candidate.dto.CandidateOfficialData;
import giunei.representa_brasil.candidate.dto.CandidateProcessedData;
import giunei.representa_brasil.candidate.dto.CandidateSearchItem;
import giunei.representa_brasil.shared.domain.Cargo;
import giunei.representa_brasil.shared.response.GroupedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/candidates")
@Tag(name = "Candidatos")
public class CandidateController {

	private final CandidateService candidateService;

	public CandidateController(CandidateService candidateService) {
		this.candidateService = candidateService;
	}

	@GetMapping
	@Operation(summary = "Buscar candidatos por cargo e filtros")
	public List<CandidateSearchItem> search(
			@RequestParam Cargo cargo,
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) String uf,
			@RequestParam(required = false) String partido) {
		return candidateService.search(nome, cargo, uf, partido);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obter perfil agrupado do candidato")
	public GroupedResponse<CandidateOfficialData, CandidateProcessedData, CandidateComplementaryData> profile(
			@PathVariable String id,
			@RequestParam(required = false) String uf) {
		return candidateService.getProfile(id, uf);
	}
}
