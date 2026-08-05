package giunei.representa_brasil.integration.senado;

import java.util.List;
import java.util.Optional;

public interface SenadoClient {

	List<SenadoSenatorSummary> listCurrentSenators();

	Optional<SenadoSenatorDetail> findSenator(long id);

	List<SenadoMatterSummary> searchMatters(String query, String type, Integer year);

	Optional<SenadoMatterDetail> findMatter(long id);
}
