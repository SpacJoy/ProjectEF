package moze_intel.projecte.integration.recipe_viewer.rei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import moze_intel.projecte.PECore;
import moze_intel.projecte.integration.recipe_viewer.FuelUpgradeRecipe;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * REI display for a collector fuel upgrade (input fuel + EMC -> upgraded fuel).
 * Mirrors {@link moze_intel.projecte.integration.recipe_viewer.emi.CollectorEmiRecipe} on top of the shared
 * {@link FuelUpgradeRecipe} data produced by {@link moze_intel.projecte.integration.recipe_viewer.RecipeViewerHelper}.
 */
public class CollectorReiDisplay extends BasicDisplay {

	public static final CategoryIdentifier<CollectorReiDisplay> CATEGORY = CategoryIdentifier.of(PECore.rl("collector"));
	//26.3 made Display#getSerializer mandatory, so every display has to carry its own codecs for syncing
	public static final DisplaySerializer<CollectorReiDisplay> SERIALIZER = DisplaySerializer.of(
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(display -> display.inputs),
					EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(display -> display.outputs),
					Identifier.CODEC.optionalFieldOf("location").forGetter(display -> display.location),
					Codec.LONG.fieldOf("upgrade_emc").forGetter(display -> display.upgradeEMC)
			).apply(instance, CollectorReiDisplay::new)),
			StreamCodec.composite(
					EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), display -> display.inputs,
					EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), display -> display.outputs,
					ByteBufCodecs.optional(Identifier.STREAM_CODEC), display -> display.location,
					ByteBufCodecs.VAR_LONG, display -> display.upgradeEMC,
					CollectorReiDisplay::new
			)
	);

	private final long upgradeEMC;

	public CollectorReiDisplay(FuelUpgradeRecipe recipe) {
		this(
				List.of(EntryIngredients.of(recipe.input().value())),
				List.of(EntryIngredients.of(recipe.output().value())),
				Optional.of(recipe.syntheticId()),
				recipe.upgradeEMC()
		);
	}

	public CollectorReiDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location, long upgradeEMC) {
		super(inputs, outputs, location);
		this.upgradeEMC = upgradeEMC;
	}

	public long getUpgradeEMC() {
		return upgradeEMC;
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return CATEGORY;
	}

	@Override
	public DisplaySerializer<? extends CollectorReiDisplay> getSerializer() {
		return SERIALIZER;
	}
}
