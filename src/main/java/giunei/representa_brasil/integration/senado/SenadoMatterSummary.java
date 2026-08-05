package giunei.representa_brasil.integration.senado;

public record SenadoMatterSummary(
		long id,
		String sigla,
		String numero,
		String ano,
		String ementa,
		String situacao
) {
}
