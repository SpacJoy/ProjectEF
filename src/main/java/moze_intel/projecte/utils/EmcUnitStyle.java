package moze_intel.projecte.utils;

/**
 * Which set of names large EMC values are rendered with.
 *
 * <p>Before this existed the names came straight from the loaded language: a Chinese client showed
 * {@code 兆 / 京 / 垓}, an English one {@code Trillion / Quadrillion}. That is still what {@link #AUTO}
 * does, but the two explicit styles let a player read the numbers in either notation regardless of
 * the language file — useful when following an English guide on a Chinese client, or the reverse.
 *
 * <p>The style is a client config value, so it only affects what the local player sees.
 */
public enum EmcUnitStyle {
	/** Follow the loaded language, exactly like before. */
	AUTO,
	/** Always use the Chinese magnitude names (兆, 京, 垓, …). */
	CHINESE,
	/** Always use the English short scale names (Trillion, Quadrillion, …). */
	ENGLISH;

	public EmcUnitStyle next() {
		return switch (this) {
			case AUTO -> CHINESE;
			case CHINESE -> ENGLISH;
			case ENGLISH -> AUTO;
		};
	}

	/** Resolves the style actually in use, falling back to {@link #AUTO} before the client config loads. */
	public static EmcUnitStyle current() {
		return moze_intel.projecte.config.ProjectEConfig.client.getEmcUnitStyle();
	}

	/** Parses a config string, tolerating hand edits; anything unrecognised means {@link #AUTO}. */
	public static EmcUnitStyle parse(String name) {
		if (name != null) {
			for (EmcUnitStyle style : values()) {
				if (style.name().equalsIgnoreCase(name.trim())) {
					return style;
				}
			}
		}
		return AUTO;
	}
}
