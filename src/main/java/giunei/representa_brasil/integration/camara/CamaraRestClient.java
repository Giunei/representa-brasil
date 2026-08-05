package giunei.representa_brasil.integration.camara;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import giunei.representa_brasil.integration.support.IntegrationSupport;
import giunei.representa_brasil.shared.domain.FonteOficial;

@Component
public class CamaraRestClient implements CamaraClient {

	private final RestClient restClient;

	public CamaraRestClient(@Qualifier("camaraHttp") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	@Cacheable(cacheNames = "camara-deputies", key = "#name + '-' + #uf + '-' + #party")
	public List<CamaraDeputySummary> searchDeputies(String name, String uf, String party) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/deputados")
				.queryParam("ordem", "ASC")
				.queryParam("ordenarPor", "nome");

		if (name != null && !name.isBlank()) {
			builder.queryParam("nome", name);
		}
		if (uf != null && !uf.isBlank()) {
			builder.queryParam("siglaUf", uf.toUpperCase());
		}
		if (party != null && !party.isBlank()) {
			builder.queryParam("siglaPartido", party.toUpperCase());
		}

		return fetchList(builder.toUriString(), "camara.searchDeputies", this::toDeputySummary);
	}

	@Override
	@Cacheable(cacheNames = "camara-deputy", key = "#id")
	public Optional<CamaraDeputyDetail> findDeputy(long id) {
		try {
			Map<String, Object> body = restClient.get()
					.uri("/deputados/{id}", id)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (body == null || !(body.get("dados") instanceof Map<?, ?> dados)) {
				return Optional.empty();
			}
			return Optional.of(toDeputyDetail(dados));
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				return Optional.empty();
			}
			throw IntegrationSupport.fromResponse(FonteOficial.CAMARA, "camara.findDeputy", ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.CAMARA, "camara.findDeputy", ex);
		}
	}

	@Override
	@Cacheable(cacheNames = "camara-propositions", key = "#query + '-' + #type + '-' + #year")
	public List<CamaraPropositionSummary> searchPropositions(String query, String type, Integer year) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/proposicoes")
				.queryParam("ordem", "DESC")
				.queryParam("ordenarPor", "id")
				.queryParam("itens", 20);

		if (query != null && !query.isBlank()) {
			builder.queryParam("keywords", query);
		}
		if (type != null && !type.isBlank()) {
			builder.queryParam("siglaTipo", type.toUpperCase());
		}
		if (year != null) {
			builder.queryParam("ano", year);
		}

		return fetchList(builder.toUriString(), "camara.searchPropositions", this::toPropositionSummary);
	}

	@Override
	@Cacheable(cacheNames = "camara-proposition", key = "#id")
	public Optional<CamaraPropositionDetail> findProposition(long id) {
		try {
			Map<String, Object> body = restClient.get()
					.uri("/proposicoes/{id}", id)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (body == null || !(body.get("dados") instanceof Map<?, ?> dados)) {
				return Optional.empty();
			}

			List<CamaraTramitation> tramitacoes = listTramitacoes(id);
			return Optional.of(toPropositionDetail(dados, tramitacoes));
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				return Optional.empty();
			}
			throw IntegrationSupport.fromResponse(FonteOficial.CAMARA, "camara.findProposition", ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.CAMARA, "camara.findProposition", ex);
		}
	}

	@Override
	@Cacheable(cacheNames = "camara-votes", key = "#propositionId")
	public List<CamaraVote> listPropositionVotes(long propositionId) {
		try {
			Map<String, Object> votacoesBody = restClient.get()
					.uri("/proposicoes/{id}/votacoes", propositionId)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (votacoesBody == null || !(votacoesBody.get("dados") instanceof List<?> votacoes) || votacoes.isEmpty()) {
				return List.of();
			}

			Object first = votacoes.getFirst();
			if (!(first instanceof Map<?, ?> votacao)) {
				return List.of();
			}

			Object votacaoId = votacao.get("id");
			if (votacaoId == null) {
				return List.of();
			}

			Map<String, Object> votosBody = restClient.get()
					.uri("/votacoes/{id}/votos", votacaoId)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (votosBody == null || !(votosBody.get("dados") instanceof List<?> votos)) {
				return List.of();
			}

			List<CamaraVote> result = new ArrayList<>();
			for (Object item : votos) {
				if (item instanceof Map<?, ?> voto) {
					result.add(toVote(voto));
				}
			}
			return Collections.unmodifiableList(result);
		}
		catch (RestClientResponseException ex) {
			throw IntegrationSupport.fromResponse(FonteOficial.CAMARA, "camara.listPropositionVotes", ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.CAMARA, "camara.listPropositionVotes", ex);
		}
	}

	private List<CamaraTramitation> listTramitacoes(long id) {
		return fetchList("/proposicoes/" + id + "/tramitacoes", "camara.tramitacoes", this::toTramitation);
	}

	private <T> List<T> fetchList(String uri, String operation, Mapper<T> mapper) {
		try {
			Map<String, Object> body = restClient.get()
					.uri(uri)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (body == null || !(body.get("dados") instanceof List<?> dados)) {
				return List.of();
			}

			List<T> result = new ArrayList<>();
			for (Object item : dados) {
				if (item instanceof Map<?, ?> map) {
					result.add(mapper.map(map));
				}
			}
			return Collections.unmodifiableList(result);
		}
		catch (RestClientResponseException ex) {
			throw IntegrationSupport.fromResponse(FonteOficial.CAMARA, operation, ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.CAMARA, operation, ex);
		}
	}

	private CamaraDeputySummary toDeputySummary(Map<?, ?> map) {
		return new CamaraDeputySummary(
				asLong(map.get("id")),
				asString(map.get("nome")),
				asString(map.get("siglaPartido")),
				asString(map.get("siglaUf")),
				asString(map.get("email")),
				asString(map.get("urlFoto")),
				asString(map.get("uri")));
	}

	private CamaraDeputyDetail toDeputyDetail(Map<?, ?> map) {
		Map<?, ?> ultimoStatus = map.get("ultimoStatus") instanceof Map<?, ?> status ? status : Map.of();
		return new CamaraDeputyDetail(
				asLong(map.get("id")),
				asString(map.get("nomeCivil")),
				asString(firstNonNull(ultimoStatus.get("nomeEleitoral"), map.get("nomeCivil"))),
				asString(ultimoStatus.get("siglaPartido")),
				asString(ultimoStatus.get("siglaUf")),
				asString(ultimoStatus.get("email")),
				asString(ultimoStatus.get("urlFoto")),
				asString(ultimoStatus.get("situacao")),
				asString(map.get("uri")),
				asString(map.get("dataNascimento")),
				asString(map.get("escolaridade")));
	}

	private CamaraPropositionSummary toPropositionSummary(Map<?, ?> map) {
		return new CamaraPropositionSummary(
				asLong(map.get("id")),
				asString(map.get("siglaTipo")),
				asInt(map.get("numero")),
				asInt(map.get("ano")),
				asString(map.get("ementa")),
				asString(map.get("uri")));
	}

	private CamaraPropositionDetail toPropositionDetail(Map<?, ?> map, List<CamaraTramitation> tramitacoes) {
		Map<?, ?> status = map.get("statusProposicao") instanceof Map<?, ?> s ? s : Map.of();
		return new CamaraPropositionDetail(
				asLong(map.get("id")),
				asString(map.get("siglaTipo")),
				asInt(map.get("numero")),
				asInt(map.get("ano")),
				asString(map.get("ementa")),
				asString(map.get("ementaDetalhada")),
				asString(status.get("descricaoSituacao")),
				asString(map.get("uri")),
				tramitacoes);
	}

	private CamaraTramitation toTramitation(Map<?, ?> map) {
		return new CamaraTramitation(
				asString(map.get("dataHora")),
				asString(map.get("descricaoSituacao")),
				asString(map.get("despacho")),
				asString(firstNonNull(map.get("siglaOrgao"), map.get("nomeOrgao"))));
	}

	private CamaraVote toVote(Map<?, ?> map) {
		Map<?, ?> deputado = map.get("deputado_") instanceof Map<?, ?> d ? d : Map.of();
		return new CamaraVote(
				asLong(deputado.get("id")),
				asString(deputado.get("nome")),
				asString(deputado.get("siglaPartido")),
				asString(deputado.get("siglaUf")),
				asString(map.get("tipoVoto")));
	}

	@FunctionalInterface
	private interface Mapper<T> {
		T map(Map<?, ?> map);
	}

	private static Object firstNonNull(Object... values) {
		for (Object value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private static String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static int asInt(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		}
		catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static long asLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(value));
		}
		catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
