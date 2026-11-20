package moze_intel.projecte.emc.mappers.recipe;

import committee.nova.mods.avaritia.api.common.crafting.ICompressorRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ExtremeSmithingRecipe;
import committee.nova.mods.avaritia.common.crafting.recipe.ITierCraftingRecipe;
import committee.nova.mods.avaritia.init.registry.ModRecipeTypes;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

@RecipeTypeMapper(requiredMods = "avaritia")
public class AvaritiaRecipeMapper extends ComponentAwareRecipeMapper {

	@Override
	public String getName() {
		return "Avaritia";
	}

	@Override
	public String getTranslationKey() {
		return "";
	}

	@Override
	public String getDescription() {
		return "Maps Re:Avaritia crafting table, compressor, and extreme smithing recipes.";
	}

	@Override
	public boolean canHandle(RecipeType<?> recipeType) {
		return recipeType == ModRecipeTypes.CRAFTING_TABLE_RECIPE.get()
			   || recipeType == ModRecipeTypes.COMPRESSOR_RECIPE.get()
			   || recipeType == ModRecipeTypes.EXTREME_SMITHING_RECIPE.get();
	}

	@Override
	public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> recipeHolder, RegistryAccess registryAccess,
			INSSFakeGroupManager fakeGroupManager) {
		if (recipeHolder.value() instanceof ICompressorRecipe recipe) {
			return handleCompressor(mapper, recipeHolder.id().identifier(), recipe, registryAccess);
		}
		return super.handleRecipe(mapper, recipeHolder, registryAccess, fakeGroupManager);
	}

	private boolean handleCompressor(IMappingCollector<NormalizedSimpleStack, Long> mapper, Identifier recipeID, ICompressorRecipe recipe,
			RegistryAccess registryAccess) {
		ItemStack result = recipe.getResultItem(registryAccess);
		if (result.isEmpty()) {
			return false;
		}
		// Re:Avaritia uses 240 ticks as one normal compression unit. Match upstream's EMC policy.
		long amount = (long) recipe.getInputCount() * recipe.getTimeCost() / 240;
		if (amount <= 0 || amount > Integer.MAX_VALUE) {
			return false;
		}
		NormalizedSimpleStack output = NSSItem.createItem(result);
		boolean handled = false;
		for (Ingredient ingredient : recipe.getIngredients()) {
			for (ItemStack input : getMatchingStacks(ingredient, recipeID)) {
				if (input.isEmpty()) {
					continue;
				}
				Object2IntMap<NormalizedSimpleStack> inputs = new Object2IntArrayMap<>(1);
				inputs.put(NSSItem.createItem(input), (int) amount);
				mapper.addConversion(result.getCount(), output, inputs);
				handled = true;
			}
		}
		return handled;
	}

	@Override
	protected Collection<Ingredient> getIngredients(Recipe<?> recipe) {
		if (recipe instanceof ExtremeSmithingRecipe smithing) {
			// The same addition is consumed independently in slots 2, 3, and 4.
			return List.of(smithing.template, smithing.base, smithing.additions, smithing.additions, smithing.additions);
		}
		return super.getIngredients(recipe);
	}

	@NotNull
	@Override
	protected ItemStack getResultItem(Recipe<?> recipe, RegistryAccess registryAccess) {
		if (recipe instanceof ITierCraftingRecipe tiered) {
			return tiered.getResultItem(registryAccess);
		}
		if (recipe instanceof ExtremeSmithingRecipe smithing) {
			return smithing.getResultItem(registryAccess);
		}
		return super.getResultItem(recipe, registryAccess);
	}
}
