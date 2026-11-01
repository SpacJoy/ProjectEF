package moze_intel.projecte.emc.mappers;

import java.util.Optional;
import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.config.PEConfigTranslations;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

@EMCMapper
public class RawMaterialsBlacklistMapper implements IEMCMapper<NormalizedSimpleStack, Long> {

	@EMCMapper.Instance
	public static final RawMaterialsBlacklistMapper INSTANCE = new RawMaterialsBlacklistMapper();

	@Override
	public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeManager recipeManager,
			RegistryAccess registryAccess, ResourceManager resourceManager) {
		Optional<Named<Item>> tag = ((net.minecraft.core.HolderGetter<net.minecraft.world.item.Item>) BuiltInRegistries.ITEM).get(ConventionalItemTags.RAW_MATERIALS);
		if (tag.isPresent()) {
			for (Holder<Item> holder : tag.get()) {
				NSSItem rawOre = NSSItem.createItem(holder);
				mapper.setValueBefore(rawOre, 0L);
				mapper.setValueAfter(rawOre, 0L);
			}
		}
	}

	@Override
	public String getName() {
		return PEConfigTranslations.MAPPING_BLACKLIST_RAW_ORE_MAPPER.title();
	}

	@Override
	public String getTranslationKey() {
		return PEConfigTranslations.MAPPING_BLACKLIST_RAW_ORE_MAPPER.getTranslationKey();
	}

	@Override
	public String getDescription() {
		return PEConfigTranslations.MAPPING_BLACKLIST_RAW_ORE_MAPPER.tooltip();
	}
}