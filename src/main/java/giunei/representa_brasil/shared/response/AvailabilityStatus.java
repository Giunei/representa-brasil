package giunei.representa_brasil.shared.response;

public record AvailabilityStatus(
		boolean official,
		boolean processed,
		boolean complementary
) {

	public static AvailabilityStatus allAvailable() {
		return new AvailabilityStatus(true, true, true);
	}

	public static AvailabilityStatus withoutComplementary() {
		return new AvailabilityStatus(true, true, false);
	}

	public AvailabilityStatus withComplementary(boolean available) {
		return new AvailabilityStatus(official, processed, available);
	}
}
