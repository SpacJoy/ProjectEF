package moze_intel.projecte.gameObjs.customRecipes;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class PhiloStoneSmeltingRecipe extends CustomRecipe {

	//Note: This recipe has no data driven inputs, so no fields to (de)serialize.
	public static final PhiloStoneSmeltingRecipe INSTANCE = new PhiloStoneSmeltingRecipe();
	public static final MapCodec<PhiloStoneSmeltingRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, PhiloStoneSmeltingRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<PhiloStoneSmeltingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	public PhiloStoneSmeltingRecipe() {
		super();
	}

	@Override
	public boolean matches(@NotNull CraftingInput inv, @NotNull Level level) {
		//If we have at least one matching recipe, return that we found a match
		return !getMatchingRecipes(inv, level).isEmpty();
	}

	@NotNull
	@Override
	public ItemStack assemble(@NotNull CraftingInput inv) {
		MinecraftServer server = PECore.getServer();
		if (server == null) {
			return ItemStack.EMPTY;
		}
		ServerLevel overworld = server.overworld();
		Set<RecipeHolder<SmeltingRecipe>> matchingRecipes = getMatchingRecipes(inv, overworld);
		if (matchingRecipes.isEmpty()) {
			return ItemStack.EMPTY;
		}
		//If we have at least one matching recipe, return the output
		//Note: It is multiplied by seven as we have seven inputs
		RecipeHolder<SmeltingRecipe> recipeHolder = matchingRecipes.iterator().next();
		//Grab the input stack that actually matches the recipe so that we can assemble the correct output
		SmeltingRecipe recipe = recipeHolder.value();
		ItemStack output = ItemStack.EMPTY;
		for (ItemStack stack : inv.items()) {
			if (!stack.isEmpty() && stack.is(PEItems.PHILOSOPHERS_STONE.get()) == false && stack.is(ItemTags.COALS) == false) {
				SingleRecipeInput furnaceInput = new SingleRecipeInput(stack);
				if (recipe.matches(furnaceInput, overworld)) {
					output = recipe.assemble(furnaceInput);
					break;
				}
			}
		}
		if (output.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return output.copyWithCount(output.getCount() * 7);
	}

	private Set<RecipeHolder<SmeltingRecipe>> getMatchingRecipes(CraftingInput inv, @NotNull Level level) {
		List<ItemStack> philoStones = new ArrayList<>();
		List<ItemStack> coals = new ArrayList<>();
		List<ItemStack> allItems = new ArrayList<>();
		for (ItemStack stack : inv.items()) {
			if (!stack.isEmpty()) {
				allItems.add(stack);
				if (allItems.size() > 9) {
					//Exit if we have more than 9 items total (for mods that may add larger crafting tables)
					return Collections.emptySet();
				}
				if (stack.is(PEItems.PHILOSOPHERS_STONE.get())) {
					philoStones.add(stack);
				}
				if (stack.is(ItemTags.COALS)) {
					coals.add(stack);
				}
			}
		}
		if (allItems.size() == 9) {
			//If we have exactly 9 items check for a matching recipe
			RecipeManager recipeManager = getRecipeManager(level);
			if (recipeManager == null) {
				return Collections.emptySet();
			}
			for (ItemStack philoStone : philoStones) {
				for (ItemStack coal : coals) {
					//Skip if the philosopher's stone is the same stack as the coal stack
					// This may be the case if a pack dev added the philosopher's stone to the coals tag
					if (philoStone != coal) {
						Set<RecipeHolder<SmeltingRecipe>> matchingRecipes = new HashSet<>();
						for (ItemStack stack : allItems) {
							//Ignore checking the piece of coal and the philosopher's stone
							if (stack != philoStone && stack != coal) {
								//And check all the other elements to find any matching recipes
								SingleRecipeInput furnaceInput = new SingleRecipeInput(stack);
								if (matchingRecipes.isEmpty()) {
									//If there are no matching recipes yet seed them with every recipe that matches this stack
									for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
										if (holder.value().getType() == RecipeType.SMELTING && holder.value() instanceof SmeltingRecipe smelting
											&& smelting.matches(furnaceInput, level)) {
											matchingRecipes.add(new RecipeHolder<>(holder.id(), smelting));
										}
									}
									if (matchingRecipes.isEmpty()) {
										return Collections.emptySet();
									}
								} else {
									//noinspection Java8CollectionRemoveIf - Capturing lambda
									for (Iterator<RecipeHolder<SmeltingRecipe>> iterator = matchingRecipes.iterator(); iterator.hasNext(); ) {
										RecipeHolder<SmeltingRecipe> recipe = iterator.next();
										if (!recipe.value().matches(furnaceInput, level)) {
											iterator.remove();
										}
									}
									//Because we might have removed some recipes, check one more time if they are empty
									if (matchingRecipes.isEmpty()) {
										//If it is exit due to there being no match
										return Collections.emptySet();
									}
								}
							}
						}
						if (!matchingRecipes.isEmpty()) {
							//We have at least one matching recipe, so return the found recipes
							return matchingRecipes;
						}
					}
				}
			}
		}
		return Collections.emptySet();
	}

	private RecipeManager getRecipeManager(@NotNull Level level) {
		if (level instanceof ServerLevel serverLevel) {
			return serverLevel.recipeAccess();
		}
		MinecraftServer server = PECore.getServer();
		return server == null ? null : server.getRecipeManager();
	}

	@NotNull
	@Override
	public RecipeSerializer<PhiloStoneSmeltingRecipe> getSerializer() {
		return SERIALIZER;
	}
}
