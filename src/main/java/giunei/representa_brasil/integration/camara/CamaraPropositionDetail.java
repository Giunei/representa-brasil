package giunei.representa_brasil.integration.camara;

import java.util.List;

public record CamaraPropositionDetail(
		long id,
		String siglaTipo,
		int numero,
		int ano,
		String ementa,
		String ementaDetalhada,
		String status,
		String uri,
		List<CamaraTramitation> tramitacoes
) {
}
