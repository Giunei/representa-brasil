package giunei.representa_brasil.integration.camara;

public record CamaraDeputyDetail(
		long id,
		String nomeCivil,
		String nomeEleitoral,
		String siglaPartido,
		String uf,
		String email,
		String fotoUrl,
		String situacao,
		String uri,
		String dataNascimento,
		String escolaridade
) {
}
