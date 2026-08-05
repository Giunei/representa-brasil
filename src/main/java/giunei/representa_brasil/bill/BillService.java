package giunei.representa_brasil.bill;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import giunei.representa_brasil.bill.dto.BillComplementaryData;
import giunei.representa_brasil.bill.dto.BillOfficialData;
import giunei.representa_brasil.bill.dto.BillProcessedData;
import giunei.representa_brasil.bill.dto.BillSearchItem;
import giunei.representa_brasil.integration.camara.CamaraClient;
import giunei.representa_brasil.integration.camara.CamaraPropositionDetail;
import giunei.representa_brasil.integration.camara.CamaraPropositionSummary;
import giunei.representa_brasil.integration.camara.CamaraVote;
import giunei.representa_brasil.integration.senado.SenadoClient;
import giunei.representa_brasil.integration.senado.SenadoMatterDetail;
import giunei.representa_brasil.integration.senado.SenadoMatterSummary;
import giunei.representa_brasil.shared.domain.FonteOficial;
import giunei.representa_brasil.shared.domain.SourceLink;
import giunei.representa_brasil.shared.exception.ResourceNotFoundException;
import giunei.representa_brasil.shared.exception.UpstreamUnavailableException;
import giunei.representa_brasil.shared.response.AvailabilityStatus;
import giunei.representa_brasil.shared.response.GroupedResponse;

@Service
public class BillService {

	private static final String LIMITATION_NEUTRAL =
			"Este serviço é informativo e neutro. Não indica posição política.";

	private final CamaraClient camaraClient;
	private final SenadoClient senadoClient;

	public BillService(CamaraClient camaraClient, SenadoClient senadoClient) {
		this.camaraClient = camaraClient;
		this.senadoClient = senadoClient;
	}

	public List<BillSearchItem> search(String query, String tipo, Integer ano, String origem) {
		String source = origem == null ? "TODAS" : origem.trim().toUpperCase(Locale.ROOT);
		List<BillSearchItem> results = new ArrayList<>();

		if ("TODAS".equals(source) || "CAMARA".equals(source)) {
			try {
				List<CamaraPropositionSummary> propositions = camaraClient.searchPropositions(query, tipo, ano);
				for (CamaraPropositionSummary item : propositions) {
					results.add(new BillSearchItem(
							BillId.camara(item.id()).value(),
							FonteOficial.CAMARA.name(),
							item.siglaTipo(),
							String.valueOf(item.numero()),
							String.valueOf(item.ano()),
							item.ementa(),
							null,
							item.uri()));
				}
			}
			catch (UpstreamUnavailableException ex) {
				// continua com Senado
			}
		}

		if ("TODAS".equals(source) || "SENADO".equals(source)) {
			try {
				List<SenadoMatterSummary> matters = senadoClient.searchMatters(query, tipo, ano);
				for (SenadoMatterSummary item : matters) {
					results.add(new BillSearchItem(
							BillId.senado(item.id()).value(),
							FonteOficial.SENADO.name(),
							item.sigla(),
							item.numero(),
							item.ano(),
							item.ementa(),
							item.situacao(),
							"https://www25.senado.leg.br/web/atividade/materias/-/materia/" + item.id()));
				}
			}
			catch (UpstreamUnavailableException ex) {
				// mantém resultados da Câmara, se houver
			}
		}

		return results;
	}

	public GroupedResponse<BillOfficialData, BillProcessedData, BillComplementaryData> getById(String id) {
		BillId billId = BillId.parse(id);
		return switch (billId.origin()) {
			case CAMARA -> fromCamara(billId.numericId());
			case SENADO -> fromSenado(billId.numericId());
		};
	}

	private GroupedResponse<BillOfficialData, BillProcessedData, BillComplementaryData> fromCamara(long id) {
		CamaraPropositionDetail detail = camaraClient.findProposition(id)
				.orElseThrow(() -> new ResourceNotFoundException("Proposição não encontrada: CAMARA-" + id));

		BillOfficialData official = new BillOfficialData(
				BillId.camara(id).value(),
				FonteOficial.CAMARA.name(),
				detail.siglaTipo(),
				String.valueOf(detail.numero()),
				String.valueOf(detail.ano()),
				detail.ementa(),
				detail.ementaDetalhada(),
				detail.status(),
				detail.uri());

		List<BillProcessedData.TimelineItem> timeline = detail.tramitacoes().stream()
				.map(t -> new BillProcessedData.TimelineItem(
						t.dataHora(),
						t.descricaoSituacao(),
						t.despacho(),
						t.orgao()))
				.toList();

		List<String> limitations = new ArrayList<>();
		limitations.add(LIMITATION_NEUTRAL);

		List<SourceLink> sources = new ArrayList<>();
		sources.add(new SourceLink(FonteOficial.CAMARA, "Proposicao", detail.uri()));

		BillComplementaryData complementary = null;
		boolean complementaryOk = true;
		BillProcessedData.VoteSummary voteSummary = new BillProcessedData.VoteSummary(0, 0, 0, 0);

		try {
			List<CamaraVote> votes = camaraClient.listPropositionVotes(id);
			int sim = 0;
			int nao = 0;
			int outros = 0;
			List<BillComplementaryData.VoteItem> voteItems = new ArrayList<>();
			for (CamaraVote vote : votes) {
				String tipo = vote.tipoVoto() == null ? "" : vote.tipoVoto().toLowerCase(Locale.ROOT);
				if (tipo.contains("sim")) {
					sim++;
				}
				else if (tipo.contains("não") || tipo.contains("nao")) {
					nao++;
				}
				else {
					outros++;
				}
				voteItems.add(new BillComplementaryData.VoteItem(
						vote.deputadoNome(),
						vote.partido(),
						vote.uf(),
						vote.tipoVoto()));
			}
			voteSummary = new BillProcessedData.VoteSummary(votes.size(), sim, nao, outros);
			complementary = new BillComplementaryData(
					voteItems,
					"Votos da votação mais recente vinculada à proposição.");
		}
		catch (UpstreamUnavailableException ex) {
			complementaryOk = false;
			limitations.add("Votos da Câmara indisponíveis no momento.");
		}

		BillProcessedData processed = new BillProcessedData(
				timeline,
				voteSummary,
				timeline.isEmpty()
						? "Sem tramitações retornadas pela fonte."
						: "Linha do tempo montada a partir das tramitações oficiais da Câmara.");

		return new GroupedResponse<>(
				official,
				processed,
				complementary,
				sources,
				limitations,
				complementaryOk ? AvailabilityStatus.allAvailable() : AvailabilityStatus.withoutComplementary());
	}

	private GroupedResponse<BillOfficialData, BillProcessedData, BillComplementaryData> fromSenado(long id) {
		SenadoMatterDetail detail = senadoClient.findMatter(id)
				.orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada: SENADO-" + id));

		String url = "https://www25.senado.leg.br/web/atividade/materias/-/materia/" + id;
		BillOfficialData official = new BillOfficialData(
				BillId.senado(id).value(),
				FonteOficial.SENADO.name(),
				detail.sigla(),
				detail.numero(),
				detail.ano(),
				detail.ementa(),
				detail.explicacaoEmenta(),
				detail.situacao(),
				url);

		List<BillProcessedData.TimelineItem> timeline = detail.tramitacoes().stream()
				.map(t -> new BillProcessedData.TimelineItem(t.data(), t.descricao(), null, t.local()))
				.toList();

		List<SourceLink> sources = List.of(new SourceLink(FonteOficial.SENADO, "Materia", url));
		List<String> limitations = List.of(
				LIMITATION_NEUTRAL,
				"Detalhamento de votos nominais no Senado pode variar conforme a matéria.");

		BillProcessedData processed = new BillProcessedData(
				timeline,
				new BillProcessedData.VoteSummary(0, 0, 0, 0),
				timeline.isEmpty()
						? "Sem movimentações retornadas pela fonte."
						: "Linha do tempo montada a partir das movimentações oficiais do Senado.");

		return new GroupedResponse<>(
				official,
				processed,
				new BillComplementaryData(List.of(), "Votos nominais do Senado não foram carregados nesta etapa."),
				sources,
				limitations,
				AvailabilityStatus.withoutComplementary());
	}

	enum BillOrigin {
		CAMARA, SENADO
	}

	record BillId(BillOrigin origin, long numericId) {

		static BillId camara(long id) {
			return new BillId(BillOrigin.CAMARA, id);
		}

		static BillId senado(long id) {
			return new BillId(BillOrigin.SENADO, id);
		}

		static BillId parse(String raw) {
			if (raw == null || raw.isBlank()) {
				throw new IllegalArgumentException("Identificador da proposição inválido.");
			}
			String value = raw.trim().toUpperCase(Locale.ROOT);
			int separator = value.indexOf('-');
			if (separator <= 0 || separator == value.length() - 1) {
				throw new IllegalArgumentException("Identificador esperado no formato ORIGEM-ID (ex.: CAMARA-123).");
			}
			BillOrigin origin = BillOrigin.valueOf(value.substring(0, separator));
			try {
				long id = Long.parseLong(value.substring(separator + 1));
				return new BillId(origin, id);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Identificador esperado no formato ORIGEM-ID (ex.: CAMARA-123).");
			}
		}

		String value() {
			return origin.name() + "-" + numericId;
		}
	}
}
