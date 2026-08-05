package giunei.representa_brasil.integration.tse;

import java.math.BigDecimal;

public record TseAsset(
		String tipo,
		String descricao,
		BigDecimal valor
) {
}
