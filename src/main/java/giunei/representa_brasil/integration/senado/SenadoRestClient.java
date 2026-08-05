package giunei.representa_brasil.integration.senado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
public class SenadoRestClient implements SenadoClient {

	private final RestClient restClient;

	public SenadoRestClient(@Qualifier("senadoHttp") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	@Cacheable(cacheNames = "senado-senators")
	public List<SenadoSenatorSummary> listCurrentSenators() {
		Map<String, Object> body = getJson("/senador/lista/atual.json", "senado.listCurrentSenators");
		List<Map<String, Object>> parlamentares = extractParlamentares(body);

		List<SenadoSenatorSummary> result = new ArrayList<>();
		for (Map<String, Object> parlamentar : parlamentares) {
			result.add(toSenatorSummary(parlamentar));
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	@Cacheable(cacheNames = "senado-senator", key = "#id")
	public Optional<SenadoSenatorDetail> findSenator(long id) {
		try {
			Map<String, Object> body = getJson("/senador/" + id + ".json", "senado.findSenator");
			Map<String, Object> parlamentar = extractSingleParlamentar(body);
			if (parlamentar == null) {
				return Optional.empty();
			}
			return Optional.of(toSenatorDetail(parlamentar));
		}
		catch (UpstreamSoftNotFoundException ex) {
			return Optional.empty();
		}
	}

	@Override
	@Cacheable(cacheNames = "senado-matters", key = "#query + '-' + #type + '-' + #year")
	public List<SenadoMatterSummary> searchMatters(String query, String type, Integer year) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/materia/pesquisa/lista.json");

		if (query != null && !query.isBlank()) {
			builder.queryParam("palavraChave", query);
		}
		if (type != null && !type.isBlank()) {
			builder.queryParam("sigla", type.toUpperCase(Locale.ROOT));
		}
		if (year != null) {
			builder.queryParam("ano", year);
		}

		Map<String, Object> body = getJson(builder.toUriString(), "senado.searchMatters");
		List<Map<String, Object>> materias = extractMaterias(body);

		List<SenadoMatterSummary> result = new ArrayList<>();
		for (Map<String, Object> materia : materias) {
			result.add(toMatterSummary(materia));
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	@Cacheable(cacheNames = "senado-matter", key = "#id")
	public Optional<SenadoMatterDetail> findMatter(long id) {
		try {
			Map<String, Object> body = getJson("/materia/" + id + ".json", "senado.findMatter");
			Map<String, Object> materia = extractSingleMateria(body);
			if (materia == null) {
				return Optional.empty();
			}

			List<SenadoTramitation> tramitacoes = listTramitacoes(id);
			return Optional.of(toMatterDetail(materia, tramitacoes));
		}
		catch (UpstreamSoftNotFoundException ex) {
			return Optional.empty();
		}
	}

	private List<SenadoTramitation> listTramitacoes(long id) {
		try {
			Map<String, Object> body = getJson("/materia/movimentacoes/" + id + ".json", "senado.tramitacoes");
			List<Map<String, Object>> items = extractNestedList(body, "MovimentacaoMateria", "Materia", "OrdensDoDia", "Ordem");
			if (items.isEmpty()) {
				items = extractNestedList(body, "MovimentacaoMateria", "Materia", "OutrasInformacoes", "Informacao");
			}

			List<SenadoTramitation> result = new ArrayList<>();
			for (Map<String, Object> item : items) {
				result.add(new SenadoTramitation(
						asString(firstNonNull(item.get("Data"), item.get("DataSessao"))),
						asString(firstNonNull(item.get("Descricao"), item.get("DescricaoSituacao"))),
						asString(firstNonNull(item.get("NomeCasa"), item.get("SiglaCasa")))));
			}
			return Collections.unmodifiableList(result);
		}
		catch (Exception ex) {
			return List.of();
		}
	}

	private Map<String, Object> getJson(String uri, String operation) {
		try {
			Map<String, Object> body = restClient.get()
					.uri(uri)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
			if (body == null) {
				throw new UpstreamSoftNotFoundException();
			}
			return body;
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				throw new UpstreamSoftNotFoundException();
			}
			throw IntegrationSupport.fromResponse(FonteOficial.SENADO, operation, ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.SENADO, operation, ex);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> extractParlamentares(Map<String, Object> body) {
		Object lista = deepGet(body, "ListaParlamentarEmExercicio", "Parlamentares", "Parlamentar");
		return asMapList(lista);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractSingleParlamentar(Map<String, Object> body) {
		Object parlamentar = deepGet(body, "DetalheParlamentar", "Parlamentar");
		if (parlamentar instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> extractMaterias(Map<String, Object> body) {
		Object lista = deepGet(body, "PesquisaBasicaMateria", "Materias", "Materia");
		if (lista == null) {
			lista = deepGet(body, "ListaMaterias", "Materias", "Materia");
		}
		return asMapList(lista);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractSingleMateria(Map<String, Object> body) {
		Object materia = deepGet(body, "DetalheMateria", "Materia");
		if (materia instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return null;
	}

	private List<Map<String, Object>> extractNestedList(Map<String, Object> body, String... path) {
		return asMapList(deepGet(body, path));
	}

	@SuppressWarnings("unchecked")
	private static Object deepGet(Map<String, Object> body, String... path) {
		Object current = body;
		for (String key : path) {
			if (!(current instanceof Map<?, ?> map)) {
				return null;
			}
			current = map.get(key);
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> asMapList(Object value) {
		if (value instanceof List<?> list) {
			List<Map<String, Object>> result = new ArrayList<>();
			for (Object item : list) {
				if (item instanceof Map<?, ?> map) {
					result.add((Map<String, Object>) map);
				}
			}
			return result;
		}
		if (value instanceof Map<?, ?> map) {
			return List.of((Map<String, Object>) map);
		}
		return List.of();
	}

	private SenadoSenatorSummary toSenatorSummary(Map<String, Object> map) {
		Map<String, Object> identificacao = asMap(map.get("IdentificacaoParlamentar"));
		Map<String, Object> mandato = asMap(map.get("Mandato"));

		return new SenadoSenatorSummary(
				asLong(identificacao.get("CodigoParlamentar")),
				asString(identificacao.get("NomeCompletoParlamentar")),
				asString(identificacao.get("NomeParlamentar")),
				asString(identificacao.get("SiglaPartidoParlamentar")),
				asString(firstNonNull(identificacao.get("UfParlamentar"), mandato.get("UfParlamentar"))),
				asString(identificacao.get("EmailParlamentar")),
				asString(identificacao.get("UrlFotoParlamentar")));
	}

	private SenadoSenatorDetail toSenatorDetail(Map<String, Object> map) {
		Map<String, Object> identificacao = asMap(map.get("IdentificacaoParlamentar"));
		Map<String, Object> dadosBasicos = asMap(map.get("DadosBasicosParlamentar"));

		return new SenadoSenatorDetail(
				asLong(identificacao.get("CodigoParlamentar")),
				asString(identificacao.get("NomeCompletoParlamentar")),
				asString(identificacao.get("NomeParlamentar")),
				asString(identificacao.get("SiglaPartidoParlamentar")),
				asString(identificacao.get("UfParlamentar")),
				asString(identificacao.get("EmailParlamentar")),
				asString(identificacao.get("UrlFotoParlamentar")),
				asString(dadosBasicos.get("SexoParlamentar")),
				asString(identificacao.get("FormaTratamento")));
	}

	private SenadoMatterSummary toMatterSummary(Map<String, Object> map) {
		Map<String, Object> identificacao = asMap(firstNonNull(map.get("IdentificacaoMateria"), map));
		Map<String, Object> situacao = asMap(map.get("SituacaoAtual"));

		return new SenadoMatterSummary(
				asLong(firstNonNull(identificacao.get("CodigoMateria"), map.get("Codigo"))),
				asString(identificacao.get("SiglaSubtipoMateria")),
				asString(identificacao.get("NumeroMateria")),
				asString(identificacao.get("AnoMateria")),
				asString(firstNonNull(map.get("EmentaMateria"), identificacao.get("DescricaoIdentificacaoMateria"))),
				asString(firstNonNull(situacao.get("DescricaoSituacao"), map.get("DescricaoSituacao"))));
	}

	private SenadoMatterDetail toMatterDetail(Map<String, Object> map, List<SenadoTramitation> tramitacoes) {
		Map<String, Object> identificacao = asMap(map.get("IdentificacaoMateria"));
		Map<String, Object> dadosBasicos = asMap(map.get("DadosBasicosMateria"));
		Map<String, Object> situacao = asMap(map.get("SituacaoAtual"));

		return new SenadoMatterDetail(
				asLong(identificacao.get("CodigoMateria")),
				asString(identificacao.get("SiglaSubtipoMateria")),
				asString(identificacao.get("NumeroMateria")),
				asString(identificacao.get("AnoMateria")),
				asString(dadosBasicos.get("EmentaMateria")),
				asString(dadosBasicos.get("ExplicacaoEmentaMateria")),
				asString(situacao.get("DescricaoSituacao")),
				tramitacoes);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object value) {
		if (value instanceof Map<?, ?> map) {
			return (Map<String, Object>) map;
		}
		return Map.of();
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

	private static final class UpstreamSoftNotFoundException extends RuntimeException {
	}
}
