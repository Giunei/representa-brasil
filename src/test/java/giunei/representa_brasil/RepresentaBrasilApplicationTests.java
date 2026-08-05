package giunei.representa_brasil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import giunei.representa_brasil.shared.domain.Cargo;
import giunei.representa_brasil.shared.response.AvailabilityStatus;

@SpringBootTest
@AutoConfigureMockMvc
class RepresentaBrasilApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		assertThat(Cargo.PRESIDENTE.isExecutivo()).isTrue();
		assertThat(Cargo.SENADOR.isParlamentar()).isTrue();
		assertThat(AvailabilityStatus.allAvailable().complementary()).isTrue();
	}

	@Test
	void healthEndpointIsUp() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void quizQuestionsAreAvailable() throws Exception {
		mockMvc.perform(get("/api/v1/quiz/questions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").exists());
	}

	@Test
	void aiSummarizeReturnsLimitationsWithoutModel() throws Exception {
		String body = """
				{
				  "tipo": "plano",
				  "texto": "Investir em educação básica e saúde preventiva.",
				  "fontes": ["https://exemplo.gov.br/plano.pdf"]
				}
				""";

		mockMvc.perform(post("/api/v1/ai/summarize")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.geradoPorIa").value(false))
				.andExpect(jsonPath("$.limitacoes").isArray());
	}
}
