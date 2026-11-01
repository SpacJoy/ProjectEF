package moze_intel.projecte.gameObjs.customRecipes;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.gameObjs.items.KleinStar;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

public class RecipeShapelessKleinStar extends WrappedShapelessRecipe {

	//Note: The wrapped shapeless recipe is serialized exactly like a vanilla shapeless recipe,
	// we just swap in our own type/behaviour after deserialization.
	public static final MapCodec<RecipeShapelessKleinStar> MAP_CODEC = ShapelessRecipe.MAP_CODEC.xmap(RecipeShapelessKleinStar::new, WrappedShapelessRecipe::getInternal);
	public static final StreamCodec<RegistryFriendlyByteBuf, RecipeShapelessKleinStar> STREAM_CODEC = ShapelessRecipe.STREAM_CODEC.map(RecipeShapelessKleinStar::new,
			WrappedShapelessRecipe::getInternal);
	public static final RecipeSerializer<RecipeShapelessKleinStar> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public RecipeShapelessKleinStar(ShapelessRecipe internal) {
		super(internal);
	}

	@NotNull
	@Override
	public RecipeSerializer<RecipeShapelessKleinStar> getSerializer() {
		return SERIALIZER;
	}

	@NotNull
	@Override
	public ItemStack assemble(@NotNull CraftingInput inv) {
		ItemStack result = getInternal().assemble(inv);
		if (result.getItem() instanceof KleinStar resultingStar) {
			long maxEmc = resultingStar.getMaximumEmc(result);
			long storedEMC = 0;
			for (ItemStack stack : inv.items()) {
				if (!stack.isEmpty() && stack.getItem() instanceof KleinStar star) {
					long inputEmc = star.getStoredEmc(stack);
					if (inputEmc >= maxEmc - storedEMC) {
						//If the emc stored in the input is more than how much we need to get to the max emc
						// just set it to the max and break out
						storedEMC = maxEmc;
						break;
					} else {
						//Otherwise we have room for the input emc, add it. We know this shouldn't overflow
						storedEMC += inputEmc;
					}
				}
			}
			result.set(PEDataComponentTypes.STORED_EMC.get(), storedEMC);
		}
		return result;
	}
}
