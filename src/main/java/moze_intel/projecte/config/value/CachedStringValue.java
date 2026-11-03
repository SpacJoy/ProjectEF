package moze_intel.projecte.config.value;

import java.util.Objects;
import moze_intel.projecte.config.IPEConfig;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Cached {@link String} config value.
 *
 * <p>Written for settings that hold one of a known set of names (such as the EMC unit style) but are
 * kept as a string so a hand-edited config file cannot make the whole spec fail to load: the reader
 * validates the name and falls back to the default.
 */
public class CachedStringValue extends CachedValue<String> {

	private boolean resolved;
	private String cachedValue;

	private CachedStringValue(IPEConfig config, ConfigValue<String> internal) {
		super(config, internal);
	}

	public static CachedStringValue wrap(IPEConfig config, ConfigValue<String> internal) {
		return new CachedStringValue(config, internal);
	}

	public String getOrDefault() {
		if (resolved || isLoaded()) {
			return get();
		}
		return internal.getDefault();
	}

	public String get() {
		if (!resolved) {
			//If we don't have a cached value or need to resolve it again, get it from the actual ConfigValue
			cachedValue = internal.get();
			resolved = true;
		}
		return cachedValue;
	}

	public void set(String value) {
		internal.set(value);
		cachedValue = value;
	}

	@Override
	protected boolean clearCachedValue(boolean checkChanged) {
		if (!resolved) {
			//Isn't cached don't need to clear it or run any invalidation listeners
			return false;
		}
		String oldCachedValue = cachedValue;
		resolved = false;
		//Return if we are meant to check the changed ones, and it is different then it used to be
		return checkChanged && !Objects.equals(oldCachedValue, get());
	}
}
