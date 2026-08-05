package giunei.representa_brasil.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	@Qualifier("tseHttp")
	RestClient tseHttp(AppProperties properties) {
		return createBuilder(properties)
				.baseUrl(properties.integration().tse().baseUrl())
				.build();
	}

	@Bean
	@Qualifier("camaraHttp")
	RestClient camaraHttp(AppProperties properties) {
		return createBuilder(properties)
				.baseUrl(properties.integration().camara().baseUrl())
				.build();
	}

	@Bean
	@Qualifier("senadoHttp")
	RestClient senadoHttp(AppProperties properties) {
		return createBuilder(properties)
				.baseUrl(properties.integration().senado().baseUrl())
				.build();
	}

	private static RestClient.Builder createBuilder(AppProperties properties) {
		Duration connectTimeout = properties.http().connectTimeout() == null
				? Duration.ofSeconds(5)
				: properties.http().connectTimeout();
		Duration readTimeout = properties.http().readTimeout() == null
				? Duration.ofSeconds(20)
				: properties.http().readTimeout();

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.build();

		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(readTimeout);

		return RestClient.builder()
				.requestFactory(requestFactory)
				.defaultHeader("User-Agent", properties.http().userAgent())
				.defaultHeader("Accept", "application/json");
	}
}
