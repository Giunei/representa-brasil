package giunei.representa_brasil.bill.dto;

public record BillOfficialData(
		String id,
		String origem,
		String tipo,
		String numero,
		String ano,
		String ementa,
		String ementaDetalhada,
		String situacao,
		String url
) {
}
