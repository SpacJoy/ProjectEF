package moze_intel.projecte.emc.components.processor;

import java.util.Optional;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.PEConfigTranslations;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@DataComponentProcessor
public class ArmorTrimProcessor extends PersistentComponentProcessor<ArmorTrim> {

	@Override
	public String getName() {
		return PEConfigTranslations.DCP_ARMOR_TRIM.title();
	}

	@Override
	public String getTranslationKey() {
		return PEConfigTranslations.DCP_ARMOR_TRIM.getTranslationKey();
	}

	@Override
	public String getDescription() {
		return PEConfigTranslations.DCP_ARMOR_TRIM.tooltip();
	}

	@Override
	@Range(from = 0, to = Long.MAX_VALUE)
	public long recalculateEMC(@NotNull ItemInfo info, @Range(from = 1, to = Long.MAX_VALUE) long currentEMC, @NotNull ArmorTrim trim) throws ArithmeticException {
		//26.3 dropped TrimMaterial#ingredient and TrimPattern#templateItem; both are resolved from data instead
		Holder<Item> material = findTrimMaterialItem(trim.material()).orElse(null);
		long materialEmc = material == null ? 0 : IEMCProxy.INSTANCE.getValue(material);
		if (materialEmc == 0) {
			//The material for the trim doesn't have an EMC value, so there is no valid EMC value for the applied trim as a whole
			return 0;
		}
		Holder<Item> template = findTrimTemplateItem(trim.pattern()).orElse(null);
		long templateEmc = template == null ? 0 : IEMCProxy.INSTANCE.getValue(template);
		if (templateEmc == 0) {
			//The template for the trim doesn't have an EMC value, and given the template is consumed: there is no valid EMC value for the applied trim as a whole
			return 0;
		}
		return Math.addExact(
				Math.addExact(currentEMC, materialEmc),
				templateEmc
		);
	}

	/**
	 * 26.3 removed the {@code ingredient} field from {@link TrimMaterial}. The item backing a material is the entry of the
	 * {@code trim_materials} tag whose path starts with the material name (for example {@code quartz} maps to {@code quartz}).
	 */
	private static Optional<Holder<Item>> findTrimMaterialItem(Holder<TrimMaterial> material) {
		String path = material.unwrapKey().map(key -> key.identifier().getPath()).orElse(null);
		if (path == null) {
			return Optional.empty();
		}
		Holder<Item> best = null;
		int bestLength = Integer.MAX_VALUE;
		for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.TRIM_MATERIALS)) {
			String itemPath = item.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
			if (itemPath.startsWith(path) && itemPath.length() < bestLength) {
				best = item;
				bestLength = itemPath.length();
			}
		}
		return Optional.ofNullable(best);
	}

	/**
	 * 26.3 removed the {@code templateItem} field from {@link TrimPattern}. Every vanilla pattern has a matching
	 * {@code <pattern>_armor_trim_smithing_template} item.
	 */
	@SuppressWarnings("unchecked")
	private static Optional<Holder<Item>> findTrimTemplateItem(Holder<TrimPattern> pattern) {
		return pattern.unwrapKey()
				.map(key -> (Holder<Item>) BuiltInRegistries.ITEM.get(Identifier.withDefaultNamespace(key.identifier().getPath() + "_armor_trim_smithing_template")).orElse(null))
				.filter(holder -> holder != null);
	}

	@Override
	protected boolean validItem(@NotNull ItemInfo info) {
		return info.getItem().is(ItemTags.TRIMMABLE_ARMOR);
	}

	@Override
	protected boolean shouldPersist(@NotNull ItemInfo info, @NotNull ArmorTrim component) {
		return true;
	}

	@Override
	protected DataComponentType<ArmorTrim> getComponentType(@NotNull ItemInfo info) {
		return DataComponents.TRIM;
	}
}