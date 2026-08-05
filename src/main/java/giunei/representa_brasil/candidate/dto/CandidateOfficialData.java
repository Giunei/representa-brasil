package giunei.representa_brasil.candidate.dto;

import java.math.BigDecimal;
import java.util.List;

import giunei.representa_brasil.shared.domain.Cargo;

public record CandidateOfficialData(
		String id,
		String nomeUrna,
		String nomeCompleto,
		Integer numero,
		String partido,
		String siglaPartido,
		Cargo cargo,
		String uf,
		String situacao,
		String coligacao,
		String viceNome,
		String vicePartido,
		String fotoUrl,
		List<DeclaredAsset> bens,
		List<OfficialDocument> documentos
) {

	public record DeclaredAsset(String tipo, String descricao, BigDecimal valor) {
	}

	public record OfficialDocument(String tipo, String nome, String url) {
	}
}
