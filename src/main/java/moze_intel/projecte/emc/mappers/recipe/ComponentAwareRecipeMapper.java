package moze_intel.projecte.emc.mappers.recipe;

import com.mojang.logging.LogUtils;
import java.util.List;
import moze_intel.projecte.PECore;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Resolves ingredient display stacks without discarding their data components. */
public abstract class ComponentAwareRecipeMapper extends BaseRecipeTypeMapper {

	@Override
	protected ItemStack[] getMatchingStacks(Ingredient ingredient, Identifier recipeID) {
		try {
			// Unlike Ingredient#items, a component ingredient's display contains its component patch.
			ContextMap context = ContextMap.EMPTY;
			List<ItemStack> matches = ingredient.display().resolveForStacks(context);
			return matches.toArray(ItemStack[]::new);
		} catch (Exception e) {
			PECore.LOGGER.error(LogUtils.FATAL_MARKER, "Error mapping recipe {}. Crashed when getting component-aware ingredient stacks.", recipeID, e);
			return new ItemStack[0];
		}
	}
}
