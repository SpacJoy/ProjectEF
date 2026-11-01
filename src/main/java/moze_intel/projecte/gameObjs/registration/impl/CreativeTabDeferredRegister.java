package moze_intel.projecte.gameObjs.registration.impl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import moze_intel.projecte.utils.text.ILangEntry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class CreativeTabDeferredRegister extends PEDeferredRegister<CreativeModeTab> {

	private final Consumer<PETabContents> addToExistingTabs;
	private final List<ResourceKey<CreativeModeTab>> existingTabsToModify;

	public CreativeTabDeferredRegister(String modid, Consumer<PETabContents> addToExistingTabs, List<ResourceKey<CreativeModeTab>> existingTabsToModify) {
		super(Registries.CREATIVE_MODE_TAB, modid);
		this.addToExistingTabs = addToExistingTabs;
		this.existingTabsToModify = existingTabsToModify;
	}

	@Override
	public void register() {
		super.register();
		for (ResourceKey<CreativeModeTab> tabKey : existingTabsToModify) {
			CreativeModeTabEvents.modifyOutputEvent(tabKey).register(entries -> addToExistingTabs.accept(new PETabContents(tabKey, entries)));
		}
	}

	/**
	 * @apiNote We manually require the title and icon to be passed so that we ensure all tabs have one.
	 */
	public PEDeferredHolder<CreativeModeTab, CreativeModeTab> registerMain(ILangEntry title, ItemLike icon, UnaryOperator<CreativeModeTab.Builder> operator) {
		return register(getNamespace(), title, icon, operator);
	}

	/**
	 * @apiNote We manually require the title and icon to be passed so that we ensure all tabs have one.
	 */
	public PEDeferredHolder<CreativeModeTab, CreativeModeTab> register(String name, ILangEntry title, ItemLike icon, UnaryOperator<CreativeModeTab.Builder> operator) {
		return register(name, () -> {
			CreativeModeTab.Builder builder = FabricCreativeModeTab.builder()
					.title(title.translate())
					.icon(() -> icon.asItem().getDefaultInstance());
			return operator.apply(builder).build();
		});
	}

	/**
	 * Mirrors the surface of the old tab contents event, wrapping the fabric per tab entry list.
	 */
	public static final class PETabContents {

		private final ResourceKey<CreativeModeTab> tabKey;
		private final FabricCreativeModeTabOutput entries;

		PETabContents(ResourceKey<CreativeModeTab> tabKey, FabricCreativeModeTabOutput entries) {
			this.tabKey = tabKey;
			this.entries = entries;
		}

		public ResourceKey<CreativeModeTab> getTabKey() {
			return tabKey;
		}

		public void accept(ItemLike item) {
			entries.accept(item);
		}

		public void accept(ItemStack stack) {
			entries.accept(stack);
		}
	}
}
