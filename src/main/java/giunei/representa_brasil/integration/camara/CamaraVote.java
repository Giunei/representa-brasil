package giunei.representa_brasil.integration.camara;

public record CamaraVote(
		long deputadoId,
		String deputadoNome,
		String partido,
		String uf,
		String tipoVoto
) {
}
