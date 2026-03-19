package net.p3pp3rf1y.sophisticatedinventoryinteractions.client;

public class SharedSearchPhraseMemory {
	private static String searchPhrase = "";

	private SharedSearchPhraseMemory() {
	}

	public static String getSearchPhrase() {
		return searchPhrase;
	}

	public static void setSearchPhrase(String searchPhrase) {
		SharedSearchPhraseMemory.searchPhrase = searchPhrase == null ? "" : searchPhrase;
	}
}
