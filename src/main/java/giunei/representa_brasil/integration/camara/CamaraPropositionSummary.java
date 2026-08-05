package giunei.representa_brasil.integration.camara;

public record CamaraPropositionSummary(
		long id,
		String siglaTipo,
		int numero,
		int ano,
		String ementa,
		String uri
) {
}
