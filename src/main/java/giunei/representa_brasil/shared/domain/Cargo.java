package giunei.representa_brasil.shared.domain;

public enum Cargo {
	PRESIDENTE(1, "Presidente", true, false),
	VICE_PRESIDENTE(2, "Vice-Presidente", true, false),
	GOVERNADOR(3, "Governador", true, false),
	VICE_GOVERNADOR(4, "Vice-Governador", true, false),
	SENADOR(5, "Senador", false, true),
	DEPUTADO_FEDERAL(6, "Deputado Federal", false, true),
	DEPUTADO_ESTADUAL(7, "Deputado Estadual", false, true),
	DEPUTADO_DISTRITAL(8, "Deputado Distrital", false, true);

	private final int codigoTse;
	private final String rotulo;
	private final boolean executivo;
	private final boolean parlamentar;

	Cargo(int codigoTse, String rotulo, boolean executivo, boolean parlamentar) {
		this.codigoTse = codigoTse;
		this.rotulo = rotulo;
		this.executivo = executivo;
		this.parlamentar = parlamentar;
	}

	public int codigoTse() {
		return codigoTse;
	}

	public String rotulo() {
		return rotulo;
	}

	public boolean isExecutivo() {
		return executivo;
	}

	public boolean isParlamentar() {
		return parlamentar;
	}

	public static Cargo fromCodigoTse(int codigo) {
		for (Cargo cargo : values()) {
			if (cargo.codigoTse == codigo) {
				return cargo;
			}
		}
		throw new IllegalArgumentException("Código de cargo TSE desconhecido: " + codigo);
	}
}
