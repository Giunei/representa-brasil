package giunei.representa_brasil.integration.tse;

import java.util.List;
import java.util.Optional;

import giunei.representa_brasil.shared.domain.Cargo;

public interface TseClient {

	List<TseCandidateSummary> listCandidates(Cargo cargo, String uf);

	Optional<TseCandidateDetail> findCandidate(String uf, long candidateId);
}
