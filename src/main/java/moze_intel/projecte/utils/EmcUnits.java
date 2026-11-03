package moze_intel.projecte.utils;

import moze_intel.projecte.PECore;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Renders the name that follows an abbreviated EMC value.
 *
 * <p>{@code TransmutationEMCFormatter} already knows how to shorten {@code 1234567890123} to
 * {@code 1.23}; the remaining question is which word goes after it. Three key namespaces exist:
 * <ul>
 *   <li>{@code emc.projecte.postfix.N} — the original, translated per language;</li>
 *   <li>{@code emc.projecte.unit.cn.N} — Chinese magnitude names, always Chinese;</li>
 *   <li>{@code emc.projecte.unit.en.N} — English short scale names, always English.</li>
 * </ul>
 *
 * <p>The English names are also passed as the translation fallback so a language that has not
 * translated the two explicit sets still renders a readable word instead of a raw key.
 */
public final class EmcUnits {

	/** English short scale, index 0 = 10^12. Kept in sync with PELangProvider#addPostfix. */
	public static final String[] ENGLISH = {
			"Trillion", "Quadrillion", "Quintillion", "Sextillion", "Septillion", "Octillion",
			"Nonillion", "Decillion", "Undecillion", "Duodecillion", "Tredecillion", "Quattuordecillion",
			"Quindecillion", "Sexdecillion", "Septendecillion", "Octodecillion", "Novemdecillion", "Vigintillion"
	};

	/** Chinese magnitude names, index 0 = 10^12. Kept in sync with the zh_cn postfix entries. */
	public static final String[] CHINESE = {
			"兆", "千兆", "百京", "十垓", "秭", "千秭",
			"百穰", "十沟", "涧", "千涧", "百正", "十载",
			"极", "千极", "百万极", "十亿极", "兆极", "千兆极"
	};

	private EmcUnits() {
	}

	/**
	 * Builds the {@code <number> <unit>} component for the given postfix index (0 = 10^12).
	 *
	 * @param index  postfix index, already validated against {@link #ENGLISH}.length
	 * @param number the shortened number, as produced by the formatter
	 */
	public static Component format(int index, String number) {
		return switch (EmcUnitStyle.current()) {
			case CHINESE -> translated("unit.cn." + index, number, CHINESE[index]);
			case ENGLISH -> translated("unit.en." + index, number, ENGLISH[index]);
			case AUTO -> translated("postfix." + index, number, ENGLISH[index]);
		};
	}

	private static Component translated(String path, String number, String fallbackName) {
		//The namespace is ours either way; the fallback only kicks in when the language lacks the key
		return Component.translatableWithFallback(Util.makeDescriptionId("emc", PECore.rl(path)), "%1$s " + fallbackName, number);
	}
}
