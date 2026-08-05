package giunei.representa_brasil.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import giunei.representa_brasil.search.dto.SearchResponse;
import giunei.representa_brasil.shared.domain.Cargo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Busca")
public class SearchController {

	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping
	@Operation(summary = "Busca unificada de candidatos e projetos")
	public SearchResponse search(
			@RequestParam String q,
			@RequestParam(required = false) Cargo cargo,
			@RequestParam(required = false) String uf,
			@RequestParam(required = false) String partido) {
		return searchService.search(q, cargo, uf, partido);
	}
}
