package giunei.representa_brasil.candidate.dto;

import giunei.representa_brasil.shared.domain.Cargo;

public record CandidateSearchItem(
		String id,
		String nomeUrna,
		String nomeCompleto,
		Integer numero,
		String partido,
		String siglaPartido,
		Cargo cargo,
		String uf,
		String situacao,
		String fotoUrl,
		String fonte
) {
}
