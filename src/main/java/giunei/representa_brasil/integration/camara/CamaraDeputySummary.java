package giunei.representa_brasil.integration.camara;

public record CamaraDeputySummary(
		long id,
		String nome,
		String siglaPartido,
		String uf,
		String email,
		String fotoUrl,
		String uri
) {
}
