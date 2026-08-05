package giunei.representa_brasil.shared.response;

import java.util.List;

import giunei.representa_brasil.shared.domain.SourceLink;

public record GroupedResponse<O, P, C>(
		O official,
		P processed,
		C complementary,
		List<SourceLink> sources,
		List<String> limitations,
		AvailabilityStatus availability
) {
}
