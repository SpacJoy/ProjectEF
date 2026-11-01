package moze_intel.projecte.emc.mappers.recipe.special;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import org.jetbrains.annotations.Nullable;

@RecipeTypeMapper
public class SuspiciousStewMapper extends SpecialRecipeMapper<ShapelessRecipe> {

	private static final Identifier SUSPICIOUS_STEW = BuiltInRegistries.ITEM.getKey(Items.SUSPICIOUS_STEW);
	/**
	 * All the vanilla suspicious stew recipes are ordinary crafting_shapeless recipes, so this mapper gets offered one recipe per
	 * flower. We only need to emit the conversions once, so this guards against doing the same work 20+ times.
	 */
	private boolean handledOnce;

	@Override
	protected Class<ShapelessRecipe> getRecipeClass() {
		//Note: Suspicious stew is now handled via data driven crafting_shapeless recipes
		return ShapelessRecipe.class;
	}

	@Override
	protected boolean handleMatchingRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> recipeHolder, RegistryAccess registryAccess,
			INSSFakeGroupManager fakeGroupManager) {
		if (handledOnce) {
			return true;
		}
		//26.3 removed the ItemTags.SMALL_FLOWERS constant; the small_flowers tag itself still ships as data
		TagKey<Item> smallFlowers = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("small_flowers"));
		Optional<Named<Item>> tag = ((HolderGetter<Item>) BuiltInRegistries.ITEM).get(smallFlowers);
		if (tag.isEmpty()) {
			return true;
		}
		handledOnce = true;
		Named<Item> flowersTag = tag.get();
		NSSItem nssBowl = NSSItem.createItem(Items.BOWL);
		NSSItem nssRedMushroom = NSSItem.createItem(Items.RED_MUSHROOM);
		NSSItem nssBrownMushroom = NSSItem.createItem(Items.BROWN_MUSHROOM);
		Map<@Nullable SuspiciousStewEffects, Object2IntMap<NormalizedSimpleStack>> knownEffects = new HashMap<>(flowersTag.size());
		for (Holder<Item> flower : flowersTag) {
			SuspiciousEffectHolder effectHolder = SuspiciousEffectHolder.tryGet(flower.value());
			knownEffects.computeIfAbsent(effectHolder == null ? null : effectHolder.getSuspiciousEffects(), k -> new Object2IntOpenHashMap<>())
					.put(NSSItem.createItem(flower), 1);
		}
		for (Map.Entry<@Nullable SuspiciousStewEffects, Object2IntMap<NormalizedSimpleStack>> entry : knownEffects.entrySet()) {
			Object2IntMap<NormalizedSimpleStack> flowers = entry.getValue();
			NSSItem nssStew = createStew(entry.getKey());
			NormalizedSimpleStack nssFlower;
			if (flowers.size() == 1) {
				nssFlower = flowers.keySet().iterator().next();
			} else {
				//Create a recipe using dummy ingredients for any stew recipes that share an effect
				// Note: We use a fake group in case any recipes just happen to use only these flowers in them
				nssFlower = fakeGroupManager.getOrCreateFakeGroupDirect(flowers, true).dummy();
			}
			mapper.addConversion(1, nssStew, EMCHelper.intMapOf(
					nssBowl, 1,
					nssRedMushroom, 1,
					nssBrownMushroom, 1,
					nssFlower, 1
			));
		}
		PECore.debugLog("{} Statistics:", getName());
		PECore.debugLog("Found {} Suspicious Stew Recipes", knownEffects.size());
		return true;
	}

	private NSSItem createStew(@Nullable SuspiciousStewEffects effects) {
		if (effects == null) {
			return NSSItem.createItem(SUSPICIOUS_STEW);
		}
		return NSSItem.createItem(SUSPICIOUS_STEW, DataComponentPatch.builder()
				.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects)
				.build()
		);
	}

	@Override
	public String getName() {
		return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SUSPICIOUS_STEW.title();
	}

	@Override
	public String getTranslationKey() {
		return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SUSPICIOUS_STEW.getTranslationKey();
	}

	@Override
	public String getDescription() {
		return PEConfigTranslations.MAPPING_CRAFTING_MAPPER_SUSPICIOUS_STEW.tooltip();
	}
}
