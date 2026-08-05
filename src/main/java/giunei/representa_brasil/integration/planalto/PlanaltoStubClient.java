package giunei.representa_brasil.integration.planalto;

import java.util.List;

import org.springframework.stereotype.Component;

import giunei.representa_brasil.config.AppProperties;

@Component
public class PlanaltoStubClient implements PlanaltoClient {

	private final AppProperties properties;

	public PlanaltoStubClient(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public boolean isEnabled() {
		return properties.integration().planalto().enabled();
	}

	@Override
	public List<PlanaltoAct> listActs(String query) {
		return List.of();
	}
}
