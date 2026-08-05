package giunei.representa_brasil.shared.exception;

import giunei.representa_brasil.shared.domain.FonteOficial;

public class UpstreamUnavailableException extends RuntimeException {

	private final FonteOficial fonte;

	public UpstreamUnavailableException(FonteOficial fonte, String message, Throwable cause) {
		super(message, cause);
		this.fonte = fonte;
	}

	public UpstreamUnavailableException(FonteOficial fonte, String message) {
		super(message);
		this.fonte = fonte;
	}

	public FonteOficial fonte() {
		return fonte;
	}
}
