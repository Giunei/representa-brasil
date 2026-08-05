package giunei.representa_brasil.integration.tse;

public record TseCandidateSummary(
		long id,
		String nomeUrna,
		String nomeCompleto,
		int numero,
		String partido,
		String siglaPartido,
		int codigoCargo,
		String cargo,
		String uf,
		String situacao
) {
}
