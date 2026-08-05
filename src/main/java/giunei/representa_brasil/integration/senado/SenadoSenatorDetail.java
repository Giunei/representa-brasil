package giunei.representa_brasil.integration.senado;

public record SenadoSenatorDetail(
		long id,
		String nome,
		String nomeParlamentar,
		String partido,
		String uf,
		String email,
		String fotoUrl,
		String sexo,
		String tratamento
) {
}
