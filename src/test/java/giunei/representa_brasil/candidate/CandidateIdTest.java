package giunei.representa_brasil.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CandidateIdTest {

	@Test
	void parsesUfAndId() {
		CandidateService.CandidateId id = CandidateService.CandidateId.parse("SP-12345");
		assertThat(id.uf()).isEqualTo("SP");
		assertThat(id.tseId()).isEqualTo(12345L);
		assertThat(id.value()).isEqualTo("SP-12345");
	}

	@Test
	void rejectsInvalidFormat() {
		assertThatThrownBy(() -> CandidateService.CandidateId.parse("12345"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
