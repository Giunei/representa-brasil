package giunei.representa_brasil.shared.domain;

public enum FonteOficial {
	TSE("Tribunal Superior Eleitoral"),
	CAMARA("Câmara dos Deputados"),
	SENADO("Senado Federal"),
	PLANALTO("Presidência da República"),
	TRANSPARENCIA("Portal da Transparência"),
	DOU("Diário Oficial da União");

	private final String nome;

	FonteOficial(String nome) {
		this.nome = nome;
	}

	public String nome() {
		return nome;
	}
}
