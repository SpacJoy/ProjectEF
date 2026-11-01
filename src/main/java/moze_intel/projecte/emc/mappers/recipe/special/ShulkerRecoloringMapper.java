package moze_intel.projecte.emc.mappers.recipe.special;

import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

@RecipeTypeMapper
public class ShulkerRecoloringMapper extends SpecialRecipeMapper<TransmuteRecipe> {

	@Override
	protected Class<TransmuteRecipe> getRecipeClass() {
		//Note: Vanilla shulker recoloring is now handled via the data driven crafting_transmute recipe
		return TransmuteRecipe.class;
	}

	@Override
	protected boolean handleMatchingRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> recipeHolder, RegistryAccess registryAccess,
			INSSFakeGroupManager fakeGroupManager) {
		//Note: All vanilla shulker recoloring recipes share the same shape (a shulker box plus one dye of the target color),
		// so instead of reading each recipe we just emit the conversion for every dye color at once.
		NSSItem nssShulker = NSSItem.createItem(Items.SHULKER_BOX);
		for (DyeColor color : Constants.COLORS) {
			Block coloredShulker = getShulkerBox(color);
			Item dye = getDye(color);
			if (coloredShulker == null || dye == null) {
				continue;
			}
			mapper.addConversion(1, NSSItem.createItem(coloredShulker), EMCHelper.intMapOf(
					nssShulker, 1,
					NSSItem.createItem(dye), 1
			));
		}
		return true;
	}

	private Block getShulkerBox(DyeColor color) {
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof ShulkerBoxBlock shulkerBox && shulkerBox.getColor() == color) {
				return block;
			}
		}
		return null;
	}

	private Item getDye(DyeColor color) {
		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof DyeItem && item.components().get(DataComponents.DYE) == color) {
				return item;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SHULKER_RECOLORING.title();
	}

	@Override
	public String getTranslationKey() {
		return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SHULKER_RECOLORING.getTranslationKey();
	}

	@Override
	public String getDescription() {
		return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SHULKER_RECOLORING.tooltip();
	}
}
