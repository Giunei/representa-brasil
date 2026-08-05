package giunei.representa_brasil.integration.support;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

import giunei.representa_brasil.shared.domain.FonteOficial;
import giunei.representa_brasil.shared.exception.UpstreamUnavailableException;

public final class IntegrationSupport {

	private IntegrationSupport() {
	}

	public static UpstreamUnavailableException wrap(FonteOficial fonte, String operation, Exception cause) {
		return new UpstreamUnavailableException(
				fonte,
				"Falha ao executar '" + operation + "'.",
				cause);
	}

	public static void ensureSuccess(FonteOficial fonte, String operation, HttpStatusCode status) {
		if (status.isError()) {
			throw new UpstreamUnavailableException(
					fonte,
					"Resposta HTTP " + status.value() + " em '" + operation + "'.");
		}
	}

	public static UpstreamUnavailableException fromResponse(
			FonteOficial fonte,
			String operation,
			RestClientResponseException ex) {
		return new UpstreamUnavailableException(
				fonte,
				"HTTP " + ex.getStatusCode().value() + " em '" + operation + "'.",
				ex);
	}
}
