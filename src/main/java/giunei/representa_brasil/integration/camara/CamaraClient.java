package giunei.representa_brasil.integration.camara;

import java.util.List;
import java.util.Optional;

public interface CamaraClient {

	List<CamaraDeputySummary> searchDeputies(String name, String uf, String party);

	Optional<CamaraDeputyDetail> findDeputy(long id);

	List<CamaraPropositionSummary> searchPropositions(String query, String type, Integer year);

	Optional<CamaraPropositionDetail> findProposition(long id);

	List<CamaraVote> listPropositionVotes(long propositionId);
}
