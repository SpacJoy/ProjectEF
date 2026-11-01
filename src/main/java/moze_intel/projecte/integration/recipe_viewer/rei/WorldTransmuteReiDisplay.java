package moze_intel.projecte.integration.recipe_viewer.rei;

import com.mojang.datafixers.util.Either;
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
import moze_intel.projecte.integration.recipe_viewer.FluidInfo;
import moze_intel.projecte.integration.recipe_viewer.WorldTransmuteEntry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * REI display for a world transmutation (philosopher's stone right click), supporting both item and fluid
 * inputs/outputs plus an optional sneak alternate output. Mirrors
 * {@link moze_intel.projecte.integration.recipe_viewer.emi.WorldTransmuteEmiRecipe}.
 */
public class WorldTransmuteReiDisplay extends BasicDisplay {

	public static final CategoryIdentifier<WorldTransmuteReiDisplay> CATEGORY = CategoryIdentifier.of(PECore.rl("world_transmutation"));
	//26.3 made Display#getSerializer mandatory, so every display has to carry its own codecs for syncing
	public static final DisplaySerializer<WorldTransmuteReiDisplay> SERIALIZER = DisplaySerializer.of(
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(display -> display.inputs),
					EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(display -> display.outputs),
					Identifier.CODEC.optionalFieldOf("location").forGetter(display -> display.location)
			).apply(instance, WorldTransmuteReiDisplay::new)),
			StreamCodec.composite(
					EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), display -> display.inputs,
					EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), display -> display.outputs,
					ByteBufCodecs.optional(Identifier.STREAM_CODEC), display -> display.location,
					WorldTransmuteReiDisplay::new
			)
	);

	public WorldTransmuteReiDisplay(WorldTransmuteEntry recipe) {
		this(
				List.of(asIngredient(recipe.input())),
				buildOutputs(recipe),
				Optional.of(recipe.syntheticId())
		);
	}

	public WorldTransmuteReiDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location) {
		super(inputs, outputs, location);
	}

	private static List<EntryIngredient> buildOutputs(WorldTransmuteEntry recipe) {
		if (recipe.altOutput() == null) {
			return List.of(asIngredient(recipe.output()));
		}
		return List.of(asIngredient(recipe.output()), asIngredient(recipe.altOutput()));
	}

	private static EntryIngredient asIngredient(Either<ItemStack, FluidInfo> ingredient) {
		return ingredient.map(EntryIngredients::of, info -> EntryIngredients.of(info.fluid(), info.amount()));
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return CATEGORY;
	}

	@Override
	public DisplaySerializer<? extends WorldTransmuteReiDisplay> getSerializer() {
		return SERIALIZER;
	}
}
