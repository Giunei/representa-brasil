package giunei.representa_brasil.candidate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import giunei.representa_brasil.candidate.dto.CandidateComplementaryData;
import giunei.representa_brasil.candidate.dto.CandidateOfficialData;
import giunei.representa_brasil.candidate.dto.CandidateProcessedData;
import giunei.representa_brasil.candidate.dto.CandidateSearchItem;
import giunei.representa_brasil.integration.camara.CamaraClient;
import giunei.representa_brasil.integration.camara.CamaraDeputyDetail;
import giunei.representa_brasil.integration.camara.CamaraDeputySummary;
import giunei.representa_brasil.integration.planalto.PlanaltoAct;
import giunei.representa_brasil.integration.planalto.PlanaltoClient;
import giunei.representa_brasil.integration.senado.SenadoClient;
import giunei.representa_brasil.integration.senado.SenadoSenatorDetail;
import giunei.representa_brasil.integration.senado.SenadoSenatorSummary;
import giunei.representa_brasil.integration.tse.TseCandidateDetail;
import giunei.representa_brasil.integration.tse.TseCandidateSummary;
import giunei.representa_brasil.integration.tse.TseClient;
import giunei.representa_brasil.shared.domain.Cargo;
import giunei.representa_brasil.shared.domain.FonteOficial;
import giunei.representa_brasil.shared.domain.SourceLink;
import giunei.representa_brasil.shared.exception.ResourceNotFoundException;
import giunei.representa_brasil.shared.exception.UpstreamUnavailableException;
import giunei.representa_brasil.shared.response.AvailabilityStatus;
import giunei.representa_brasil.shared.response.GroupedResponse;

@Service
public class CandidateService {

	private static final String LIMITATION_NEUTRAL =
			"Este serviço é informativo e neutro. Não indica em quem votar.";
	private static final String LIMITATION_AI =
			"Resumos por IA, quando existirem, baseiam-se apenas nos documentos oficiais encontrados.";
	private static final String LIMITATION_EXECUTIVE =
			"Cumprimento de promessas de campanha não é avaliado: é subjetivo. "
					+ "Exibimos o plano registrado no TSE e atos públicos relacionados, quando houver.";

	private final TseClient tseClient;
	private final CamaraClient camaraClient;
	private final SenadoClient senadoClient;
	private final PlanaltoClient planaltoClient;

	public CandidateService(
			TseClient tseClient,
			CamaraClient camaraClient,
			SenadoClient senadoClient,
			PlanaltoClient planaltoClient) {
		this.tseClient = tseClient;
		this.camaraClient = camaraClient;
		this.senadoClient = senadoClient;
		this.planaltoClient = planaltoClient;
	}

	public List<CandidateSearchItem> search(String nome, Cargo cargo, String uf, String partido) {
		if (cargo == null) {
			throw new IllegalArgumentException("Parâmetro 'cargo' é obrigatório.");
		}

		List<TseCandidateSummary> candidates = tseClient.listCandidates(cargo, uf);
		String nomeFilter = normalize(nome);
		String partidoFilter = normalize(partido);

		return candidates.stream()
				.filter(item -> matchesName(item, nomeFilter))
				.filter(item -> matchesParty(item, partidoFilter))
				.sorted(Comparator.comparing(TseCandidateSummary::nomeUrna, Comparator.nullsLast(String::compareToIgnoreCase)))
				.map(this::toSearchItem)
				.toList();
	}

	public GroupedResponse<CandidateOfficialData, CandidateProcessedData, CandidateComplementaryData> getProfile(
			String id,
			String uf) {
		CandidateId candidateId = CandidateId.parse(id);
		TseCandidateDetail detail = tseClient.findCandidate(
						uf == null || uf.isBlank() ? candidateId.uf() : uf,
						candidateId.tseId())
				.orElseThrow(() -> new ResourceNotFoundException("Candidato não encontrado: " + id));

		Cargo cargo = safeCargo(detail.codigoCargo());
		CandidateOfficialData official = toOfficial(detail, cargo);
		CandidateProcessedData processed = toProcessed(detail, cargo);

		List<SourceLink> sources = new ArrayList<>();
		sources.add(new SourceLink(FonteOficial.TSE, "DivulgaCandContas", detail.detalheUrl()));
		for (var documento : detail.documentos()) {
			if (documento.url() != null && !documento.url().isBlank()) {
				sources.add(new SourceLink(FonteOficial.TSE, documento.nome(), documento.url()));
			}
		}

		List<String> limitations = new ArrayList<>();
		limitations.add(LIMITATION_NEUTRAL);
		limitations.add(LIMITATION_AI);
		if (cargo != null && cargo.isExecutivo()) {
			limitations.add(LIMITATION_EXECUTIVE);
		}

		boolean complementaryOk = true;
		CandidateComplementaryData complementary = null;
		try {
			complementary = buildComplementary(detail, cargo, sources, limitations);
		}
		catch (UpstreamUnavailableException ex) {
			complementaryOk = false;
			limitations.add("Dados complementares de " + ex.fonte().nome() + " indisponíveis no momento.");
		}

		AvailabilityStatus availability = complementaryOk
				? AvailabilityStatus.allAvailable()
				: AvailabilityStatus.withoutComplementary();

		return new GroupedResponse<>(official, processed, complementary, sources, limitations, availability);
	}

	private CandidateComplementaryData buildComplementary(
			TseCandidateDetail detail,
			Cargo cargo,
			List<SourceLink> sources,
			List<String> limitations) {
		CandidateComplementaryData.ParliamentaryHistory history = null;

		if (cargo == Cargo.DEPUTADO_FEDERAL || (cargo != null && cargo.isExecutivo())) {
			history = findCamaraHistory(detail, sources);
		}
		if (history == null && (cargo == Cargo.SENADOR || (cargo != null && cargo.isExecutivo()))) {
			history = findSenadoHistory(detail, sources);
		}

		List<CandidateComplementaryData.ExecutiveAct> acts = List.of();
		if (cargo != null && cargo.isExecutivo() && planaltoClient.isEnabled()) {
			List<PlanaltoAct> planaltoActs = planaltoClient.listActs(detail.nomeUrna());
			acts = planaltoActs.stream()
					.map(act -> new CandidateComplementaryData.ExecutiveAct(
							act.tipo(), act.titulo(), act.data(), act.url()))
					.toList();
			for (var act : acts) {
				if (act.url() != null) {
					sources.add(new SourceLink(FonteOficial.PLANALTO, act.titulo(), act.url()));
				}
			}
		}
		else if (cargo != null && cargo.isExecutivo()) {
			limitations.add("Integração com atos do Executivo ainda não habilitada.");
		}

		if (history == null && acts.isEmpty()) {
			return null;
		}
		return new CandidateComplementaryData(history, acts);
	}

	private CandidateComplementaryData.ParliamentaryHistory findCamaraHistory(
			TseCandidateDetail detail,
			List<SourceLink> sources) {
		List<CamaraDeputySummary> deputies = camaraClient.searchDeputies(detail.nomeUrna(), detail.uf(), detail.siglaPartido());
		CamaraDeputySummary match = deputies.stream()
				.filter(item -> namesMatch(detail.nomeUrna(), item.nome()) || namesMatch(detail.nomeCompleto(), item.nome()))
				.findFirst()
				.orElse(null);

		if (match == null) {
			return null;
		}

		CamaraDeputyDetail full = camaraClient.findDeputy(match.id()).orElse(null);
		sources.add(new SourceLink(FonteOficial.CAMARA, "Perfil na Câmara", match.uri()));

		return new CandidateComplementaryData.ParliamentaryHistory(
				FonteOficial.CAMARA.nome(),
				full != null ? full.nomeEleitoral() : match.nome(),
				full != null ? full.siglaPartido() : match.siglaPartido(),
				full != null ? full.uf() : match.uf(),
				match.uri(),
				List.of("Histórico parlamentar obtido por correspondência de nome/partido/UF. Confira a fonte oficial."));
	}

	private CandidateComplementaryData.ParliamentaryHistory findSenadoHistory(
			TseCandidateDetail detail,
			List<SourceLink> sources) {
		List<SenadoSenatorSummary> senators = senadoClient.listCurrentSenators();
		SenadoSenatorSummary match = senators.stream()
				.filter(item -> namesMatch(detail.nomeUrna(), item.nomeParlamentar())
						|| namesMatch(detail.nomeUrna(), item.nome())
						|| namesMatch(detail.nomeCompleto(), item.nome()))
				.filter(item -> detail.uf() == null || detail.uf().equalsIgnoreCase(item.uf()) || "BR".equalsIgnoreCase(detail.uf()))
				.findFirst()
				.orElse(null);

		if (match == null) {
			return null;
		}

		SenadoSenatorDetail full = senadoClient.findSenator(match.id()).orElse(null);
		String perfilUrl = "https://www25.senado.leg.br/web/senadores/senador/-/perfil/" + match.id();
		sources.add(new SourceLink(FonteOficial.SENADO, "Perfil no Senado", perfilUrl));

		return new CandidateComplementaryData.ParliamentaryHistory(
				FonteOficial.SENADO.nome(),
				full != null ? full.nomeParlamentar() : match.nomeParlamentar(),
				full != null ? full.partido() : match.partido(),
				full != null ? full.uf() : match.uf(),
				perfilUrl,
				List.of("Histórico parlamentar obtido por correspondência de nome/partido/UF. Confira a fonte oficial."));
	}

	private CandidateSearchItem toSearchItem(TseCandidateSummary item) {
		return new CandidateSearchItem(
				CandidateId.of(item.uf(), item.id()).value(),
				item.nomeUrna(),
				item.nomeCompleto(),
				item.numero(),
				item.partido(),
				item.siglaPartido(),
				safeCargo(item.codigoCargo()),
				item.uf(),
				item.situacao(),
				null,
				FonteOficial.TSE.name());
	}

	private CandidateOfficialData toOfficial(TseCandidateDetail detail, Cargo cargo) {
		return new CandidateOfficialData(
				CandidateId.of(detail.uf(), detail.id()).value(),
				detail.nomeUrna(),
				detail.nomeCompleto(),
				detail.numero(),
				detail.partido(),
				detail.siglaPartido(),
				cargo,
				detail.uf(),
				detail.situacao(),
				detail.coligacao(),
				detail.viceNome(),
				detail.vicePartido(),
				detail.fotoUrl(),
				detail.bens().stream()
						.map(b -> new CandidateOfficialData.DeclaredAsset(b.tipo(), b.descricao(), b.valor()))
						.toList(),
				detail.documentos().stream()
						.map(d -> new CandidateOfficialData.OfficialDocument(d.tipo(), d.nome(), d.url()))
						.toList());
	}

	private CandidateProcessedData toProcessed(TseCandidateDetail detail, Cargo cargo) {
		List<CandidateProcessedData.TimelineEvent> timeline = new ArrayList<>();
		timeline.add(new CandidateProcessedData.TimelineEvent(
				null,
				"Candidatura registrada",
				detail.cargo() + " — " + detail.situacao(),
				"CANDIDATURA"));

		if (detail.viceNome() != null) {
			timeline.add(new CandidateProcessedData.TimelineEvent(
					null,
					"Vice registrado",
					detail.viceNome() + (detail.vicePartido() != null ? " (" + detail.vicePartido() + ")" : ""),
					"VICE"));
		}

		boolean hasPlan = detail.documentos().stream()
				.anyMatch(doc -> {
					String blob = ((doc.tipo() == null ? "" : doc.tipo()) + " " + (doc.nome() == null ? "" : doc.nome()))
							.toLowerCase(Locale.ROOT);
					return blob.contains("proposta") || blob.contains("governo") || blob.contains("plano");
				});

		String resumo;
		if (cargo != null && cargo.isExecutivo()) {
			resumo = "Perfil executivo com dados oficiais do TSE"
					+ (hasPlan ? ", incluindo plano de governo registrado." : ".");
		}
		else if (cargo != null && cargo.isParlamentar()) {
			resumo = "Perfil parlamentar com dados oficiais do TSE. "
					+ "Votações e proposições aparecem quando houver vínculo com Câmara ou Senado.";
		}
		else {
			resumo = "Dados oficiais do TSE organizados para consulta.";
		}

		return new CandidateProcessedData(
				timeline,
				new CandidateProcessedData.CandidateStats(
						detail.bens().size(),
						detail.documentos().size(),
						hasPlan,
						false),
				resumo);
	}

	private static boolean matchesName(TseCandidateSummary item, String filter) {
		if (filter == null) {
			return true;
		}
		return contains(item.nomeUrna(), filter) || contains(item.nomeCompleto(), filter);
	}

	private static boolean matchesParty(TseCandidateSummary item, String filter) {
		if (filter == null) {
			return true;
		}
		return contains(item.siglaPartido(), filter) || contains(item.partido(), filter);
	}

	private static boolean namesMatch(String left, String right) {
		String a = normalize(left);
		String b = normalize(right);
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b) || a.contains(b) || b.contains(a);
	}

	private static boolean contains(String value, String filter) {
		String normalized = normalize(value);
		return normalized != null && normalized.contains(filter);
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static Cargo safeCargo(int codigo) {
		try {
			return Cargo.fromCodigoTse(codigo);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	record CandidateId(String uf, long tseId) {

		static CandidateId of(String uf, long tseId) {
			String scope = uf == null || uf.isBlank() ? "BR" : uf.toUpperCase(Locale.ROOT);
			return new CandidateId(scope, tseId);
		}

		static CandidateId parse(String raw) {
			if (raw == null || raw.isBlank()) {
				throw new IllegalArgumentException("Identificador do candidato inválido.");
			}
			String value = raw.trim();
			int separator = value.lastIndexOf('-');
			if (separator <= 0 || separator == value.length() - 1) {
				throw new IllegalArgumentException("Identificador esperado no formato UF-ID (ex.: SP-123).");
			}
			String uf = value.substring(0, separator).toUpperCase(Locale.ROOT);
			try {
				long id = Long.parseLong(value.substring(separator + 1));
				return new CandidateId(uf, id);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Identificador esperado no formato UF-ID (ex.: SP-123).");
			}
		}

		String value() {
			return uf + "-" + tseId;
		}
	}
}
