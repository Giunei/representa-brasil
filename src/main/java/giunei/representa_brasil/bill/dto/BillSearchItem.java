package giunei.representa_brasil.bill.dto;

public record BillSearchItem(
		String id,
		String origem,
		String tipo,
		String numero,
		String ano,
		String ementa,
		String situacao,
		String url
) {
}
