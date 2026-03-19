package net.p3pp3rf1y.sophisticatedinventoryinteractions.common.eligibility;

public record EligibilityDecision(boolean eligible, String reason) {
	public static EligibilityDecision allow(String reason) {
		return new EligibilityDecision(true, reason);
	}

	public static EligibilityDecision deny(String reason) {
		return new EligibilityDecision(false, reason);
	}
}
