package giunei.representa_brasil.integration.tse;

import java.math.BigDecimal;
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

import giunei.representa_brasil.config.AppProperties;
import giunei.representa_brasil.integration.support.IntegrationSupport;
import giunei.representa_brasil.shared.domain.Cargo;
import giunei.representa_brasil.shared.domain.FonteOficial;

@Component
public class TseRestClient implements TseClient {

	private static final String OPERATION_LIST = "tse.listCandidates";
	private static final String OPERATION_DETAIL = "tse.findCandidate";

	private final RestClient restClient;
	private final AppProperties properties;

	public TseRestClient(@Qualifier("tseHttp") RestClient restClient, AppProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	@Override
	@Cacheable(cacheNames = "tse-candidates", key = "#cargo.name() + '-' + #uf")
	public List<TseCandidateSummary> listCandidates(Cargo cargo, String uf) {
		String scope = resolveScope(cargo, uf);
		String path = "/candidatura/listar/{ano}/{scope}/{eleicao}/{cargo}/candidatos"
				.replace("{ano}", String.valueOf(properties.election().year()))
				.replace("{scope}", scope)
				.replace("{eleicao}", String.valueOf(properties.election().id()))
				.replace("{cargo}", String.valueOf(cargo.codigoTse()));

		try {
			Map<String, Object> body = restClient.get()
					.uri(path)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (body == null) {
				return List.of();
			}

			Object candidatos = body.get("candidatos");
			if (!(candidatos instanceof List<?> list)) {
				return List.of();
			}

			List<TseCandidateSummary> result = new ArrayList<>();
			for (Object item : list) {
				if (item instanceof Map<?, ?> map) {
					result.add(toSummary(map, cargo, scope));
				}
			}
			return Collections.unmodifiableList(result);
		}
		catch (RestClientResponseException ex) {
			throw IntegrationSupport.fromResponse(FonteOficial.TSE, OPERATION_LIST, ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.TSE, OPERATION_LIST, ex);
		}
	}

	@Override
	@Cacheable(cacheNames = "tse-candidate", key = "#uf + '-' + #candidateId")
	public Optional<TseCandidateDetail> findCandidate(String uf, long candidateId) {
		String scope = uf == null || uf.isBlank() ? "BR" : uf.toUpperCase();
		String path = "/candidatura/buscar/{ano}/{scope}/{eleicao}/candidato/{id}"
				.replace("{ano}", String.valueOf(properties.election().year()))
				.replace("{scope}", scope)
				.replace("{eleicao}", String.valueOf(properties.election().id()))
				.replace("{id}", String.valueOf(candidateId));

		try {
			Map<String, Object> body = restClient.get()
					.uri(path)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});

			if (body == null || body.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(toDetail(body, scope));
		}
		catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				return Optional.empty();
			}
			throw IntegrationSupport.fromResponse(FonteOficial.TSE, OPERATION_DETAIL, ex);
		}
		catch (RestClientException ex) {
			throw IntegrationSupport.wrap(FonteOficial.TSE, OPERATION_DETAIL, ex);
		}
	}

	private static String resolveScope(Cargo cargo, String uf) {
		if (cargo == Cargo.PRESIDENTE || cargo == Cargo.VICE_PRESIDENTE) {
			return "BR";
		}
		if (uf == null || uf.isBlank()) {
			throw new IllegalArgumentException("UF é obrigatória para o cargo " + cargo.rotulo());
		}
		return uf.toUpperCase();
	}

	private TseCandidateSummary toSummary(Map<?, ?> map, Cargo cargo, String scope) {
		return new TseCandidateSummary(
				asLong(map.get("id")),
				asString(map.get("nomeUrna")),
				asString(firstNonNull(map.get("nomeCompleto"), map.get("nome"))),
				asInt(map.get("numero")),
				asString(firstNonNull(map.get("partido"), nested(map, "partido", "nome"))),
				asString(firstNonNull(map.get("partido"), nested(map, "partido", "sigla"), map.get("sgPartido"))),
				cargo.codigoTse(),
				cargo.rotulo(),
				scope,
				asString(firstNonNull(map.get("descricaoSituacao"), map.get("situacao")))
		);
	}

	@SuppressWarnings("unchecked")
	private TseCandidateDetail toDetail(Map<String, Object> map, String scope) {
		int codigoCargo = asInt(firstNonNull(map.get("codigoCargo"), map.get("cargo")));
		Cargo cargo;
		try {
			cargo = Cargo.fromCodigoTse(codigoCargo);
		}
		catch (IllegalArgumentException ex) {
			cargo = null;
		}

		List<TseAsset> bens = new ArrayList<>();
		Object bensRaw = map.get("bens");
		if (bensRaw instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof Map<?, ?> asset) {
					bens.add(new TseAsset(
							asString(asset.get("tipo")),
							asString(firstNonNull(asset.get("descricao"), asset.get("descricaoDeBem"))),
							asBigDecimal(firstNonNull(asset.get("valor"), asset.get("valorBem")))));
				}
			}
		}

		List<TseDocument> documentos = new ArrayList<>();
		Object arquivos = map.get("arquivos");
		if (arquivos instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof Map<?, ?> doc) {
					documentos.add(new TseDocument(
							asString(firstNonNull(doc.get("tipo"), doc.get("codTipo"))),
							asString(firstNonNull(doc.get("nome"), doc.get("nomeArquivo"))),
							asString(firstNonNull(doc.get("url"), doc.get("urlCompleta")))));
				}
			}
		}

		Object vices = map.get("vices");
		String viceNome = null;
		String vicePartido = null;
		if (vices instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> vice) {
			viceNome = asString(firstNonNull(vice.get("nomeUrna"), vice.get("nomeCompleto")));
			vicePartido = asString(firstNonNull(vice.get("partido"), nested(vice, "partido", "sigla")));
		}

		String detalheUrl = properties.integration().tse().baseUrl()
				+ "/candidatura/buscar/"
				+ properties.election().year() + "/"
				+ scope + "/"
				+ properties.election().id() + "/candidato/"
				+ asLong(map.get("id"));

		return new TseCandidateDetail(
				asLong(map.get("id")),
				asString(map.get("nomeUrna")),
				asString(firstNonNull(map.get("nomeCompleto"), map.get("nome"))),
				asInt(map.get("numero")),
				asString(firstNonNull(map.get("nomePartido"), nested(map, "partido", "nome"))),
				asString(firstNonNull(map.get("sgPartido"), nested(map, "partido", "sigla"))),
				codigoCargo,
				cargo != null ? cargo.rotulo() : asString(map.get("cargo")),
				scope,
				asString(firstNonNull(map.get("descricaoSituacao"), map.get("situacao"))),
				asString(firstNonNull(map.get("nomeColigacao"), map.get("composicaoColigacao"))),
				viceNome,
				vicePartido,
				Collections.unmodifiableList(bens),
				Collections.unmodifiableList(documentos),
				asString(firstNonNull(map.get("fotoUrl"), map.get("urlFoto"))),
				detalheUrl
		);
	}

	private static Object nested(Map<?, ?> map, String parent, String child) {
		Object value = map.get(parent);
		if (value instanceof Map<?, ?> nested) {
			return nested.get(child);
		}
		return null;
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

	private static BigDecimal asBigDecimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		try {
			return new BigDecimal(String.valueOf(value).replace(",", "."));
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}
}
