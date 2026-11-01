package moze_intel.projecte.emc.mappers;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSFluid;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.PotionsPredicate;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

@EMCMapper
public class BrewingMapper implements IEMCMapper<NormalizedSimpleStack, Long> {

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeManager recipeManager,
			RegistryAccess registryAccess, ResourceManager resourceManager) {
		//Add conversion for empty bottle + water to water bottle
		//Note: NSS fluid amounts are in millibuckets (1 bucket = 1000 mB), so a water bottle holds 1000 / 3 mB of water
		mapper.addConversion(1, NSSItem.createItem(PotionContents.createItemStack(Items.POTION, Potions.WATER)), EMCHelper.intMapOf(
				NSSItem.createItem(Items.GLASS_BOTTLE), 1,
				NSSFluid.createTag(FluidTags.WATER), 1_000 / 3
		));

		int recipeCount = 0;
		//26.3 turned brewing into ordinary data driven recipes (RecipeType.BREWING), replacing the hardcoded PotionBrewing
		//mix lists that used to hang off MinecraftServer. Walking the recipe registry also picks up modded brewing recipes,
		//and evaluating each input/reagent pair keeps the previous "probe every combination" behaviour.
		for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
			if (!(holder.value() instanceof BrewingRecipe recipe)) {
				continue;
			}
			List<ItemStack> inputs = matchingStacks(recipe.getInput());
			List<ItemStack> reagents = matchingStacks(recipe.getReagent());
			for (ItemStack input : inputs) {
				for (ItemStack reagent : reagents) {
					BrewingInput brewingInput = new BrewingInput(input, reagent);
					if (!recipe.matches(brewingInput)) {
						continue;
					}
					ItemStack output = recipe.assemble(brewingInput);
					if (output.isEmpty()) {
						continue;
					}
					//Add the conversion, 3 input + reagent = 3 y output as the output technically could be stacked
					mapper.addConversion(3 * output.getCount(), NSSItem.createItem(output), EMCHelper.intMapOf(
							NSSItem.createItem(input), 3,
							NSSItem.createItem(reagent), 1
					));
					recipeCount++;
				}
			}
		}
		PECore.debugLog("{} Statistics:", getName());
		PECore.debugLog("Found {} Brewing Recipes", recipeCount);
	}

	/**
	 * Expands a potion bound brewing ingredient into every concrete stack it matches, otherwise returns the plain item stacks.
	 */
	private static List<ItemStack> matchingStacks(PotionIngredient potionIngredient) {
		Ingredient ingredient = potionIngredient.ingredient();
		List<Item> items = ingredient.items().map(Holder::value).toList();
		PotionsPredicate predicate = potionIngredient.potions().orElse(null);
		HolderSet<Potion> potions = predicate == null ? null : predicate.potions().orElse(null);
		if (potions == null) {
			List<ItemStack> stacks = new ArrayList<>(items.size());
			for (Item item : items) {
				stacks.add(new ItemStack(item));
			}
			return stacks;
		}
		List<ItemStack> stacks = new ArrayList<>(items.size() * potions.size());
		for (Holder<Potion> potion : potions) {
			PotionContents contents = new PotionContents(potion);
			for (Item item : items) {
				ItemStack stack = new ItemStack(item);
				stack.set(DataComponents.POTION_CONTENTS, contents);
				stacks.add(stack);
			}
		}
		return stacks;
	}

	@Override
	public String getName() {
		return PEConfigTranslations.MAPPING_BREWING_MAPPER.title();
	}

	@Override
	public String getTranslationKey() {
		return PEConfigTranslations.MAPPING_BREWING_MAPPER.getTranslationKey();
	}

	@Override
	public String getDescription() {
		return PEConfigTranslations.MAPPING_BREWING_MAPPER.tooltip();
	}
}
