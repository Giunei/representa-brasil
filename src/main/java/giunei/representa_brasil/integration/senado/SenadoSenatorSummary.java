package giunei.representa_brasil.integration.senado;

public record SenadoSenatorSummary(
		long id,
		String nome,
		String nomeParlamentar,
		String partido,
		String uf,
		String email,
		String fotoUrl
) {
}
