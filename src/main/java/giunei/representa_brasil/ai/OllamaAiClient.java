package giunei.representa_brasil.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import giunei.representa_brasil.config.AppProperties;

@Component
public class OllamaAiClient implements AiClient {

	private final AppProperties properties;

	public OllamaAiClient(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public boolean isEnabled() {
		return properties.ai().enabled();
	}

	@Override
	public String summarize(String tipo, String texto, List<String> fontes) {
		if (!isEnabled()) {
			return null;
		}
		return null;
	}
}
