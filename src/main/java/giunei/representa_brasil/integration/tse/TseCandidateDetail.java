package giunei.representa_brasil.integration.tse;

import java.util.List;

public record TseCandidateDetail(
		long id,
		String nomeUrna,
		String nomeCompleto,
		int numero,
		String partido,
		String siglaPartido,
		int codigoCargo,
		String cargo,
		String uf,
		String situacao,
		String coligacao,
		String viceNome,
		String vicePartido,
		List<TseAsset> bens,
		List<TseDocument> documentos,
		String fotoUrl,
		String detalheUrl
) {
}
