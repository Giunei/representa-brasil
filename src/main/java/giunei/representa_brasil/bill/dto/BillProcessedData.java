package giunei.representa_brasil.bill.dto;

import java.util.List;

public record BillProcessedData(
		List<TimelineItem> linhaDoTempo,
		VoteSummary resumoVotos,
		String resumoTramitacao
) {

	public record TimelineItem(String data, String titulo, String detalhe, String local) {
	}

	public record VoteSummary(int total, int sim, int nao, int outros) {
	}
}
