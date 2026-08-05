package giunei.representa_brasil.bill.dto;

import java.util.List;

public record BillComplementaryData(
		List<VoteItem> votos,
		String observacao
) {

	public record VoteItem(
			String parlamentar,
			String partido,
			String uf,
			String voto
	) {
	}
}
