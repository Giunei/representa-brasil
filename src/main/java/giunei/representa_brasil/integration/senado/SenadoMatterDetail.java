package giunei.representa_brasil.integration.senado;

import java.util.List;

public record SenadoMatterDetail(
		long id,
		String sigla,
		String numero,
		String ano,
		String ementa,
		String explicacaoEmenta,
		String situacao,
		List<SenadoTramitation> tramitacoes
) {
}
