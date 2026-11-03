package moze_intel.projecte.config;

import moze_intel.projecte.config.value.CachedBooleanValue;
import moze_intel.projecte.config.value.CachedStringValue;
import moze_intel.projecte.utils.EmcUnitStyle;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * For config options that only the client cares about
 */
public class ClientConfig extends BasePEConfig {

	private final ModConfigSpec configSpec;

	public final CachedBooleanValue emcToolTips;
	public final CachedBooleanValue shiftEmcToolTips;
	public final CachedBooleanValue shiftLearnedToolTips;
	public final CachedBooleanValue pedestalToolTips;
	public final CachedBooleanValue statToolTips;
	public final CachedBooleanValue tagToolTips;

	public final CachedBooleanValue pulsatingOverlay;
	/** Which set of names large EMC values are abbreviated with; see {@link EmcUnitStyle}. */
	public final CachedStringValue emcUnit;

	ClientConfig() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

		pulsatingOverlay = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_PHILO_OVERLAY.applyToBuilder(builder).define("pulsatingOverlay", false));
		//Stored as a name rather than an enum so a hand-edited file can never break spec loading
		emcUnit = CachedStringValue.wrap(this, PEConfigTranslations.CLIENT_EMC_UNIT.applyToBuilder(builder).define("emcUnit", EmcUnitStyle.AUTO.name()));

		PEConfigTranslations.CLIENT_TOOLTIPS.applyToBuilder(builder).push("tooltips");
		emcToolTips = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_TOOLTIPS_EMC.applyToBuilder(builder).define("emc", true));
		shiftEmcToolTips = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_TOOLTIPS_EMC_SHIFT.applyToBuilder(builder)
				.define("shift_emc", false));
		shiftLearnedToolTips = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_TOOLTIPS_LEARNED_SHIFT.applyToBuilder(builder)
				.define("shift_learned", true));
		pedestalToolTips = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_TOOLTIPS_PEDESTAL.applyToBuilder(builder)
				.define("pedestal", true));
		statToolTips = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_TOOLTIPS_STATS.applyToBuilder(builder).define("statToolTips", true));
		tagToolTips = CachedBooleanValue.wrap(this, PEConfigTranslations.CLIENT_TOOLTIPS_TAGS.applyToBuilder(builder).define("tag", false));
		builder.pop();

		configSpec = builder.build();
	}

	@Override
	public String getFileName() {
		return "client";
	}

	@Override
	public String getTranslation() {
		return "Client Config";
	}

	@Override
	public ModConfigSpec getConfigSpec() {
		return configSpec;
	}

	@Override
	public ModConfig.Type getConfigType() {
		return ModConfig.Type.CLIENT;
	}

	/** The unit style as an enum, with a safe fallback for unrecognised values. */
	public EmcUnitStyle getEmcUnitStyle() {
		return EmcUnitStyle.parse(emcUnit.getOrDefault());
	}

	public void setEmcUnitStyle(EmcUnitStyle style) {
		if (!isLoaded()) {
			//Writing through ModConfigSpec needs the config file assigned; nothing to persist without it
			return;
		}
		emcUnit.set(style.name());
		save();
	}
}