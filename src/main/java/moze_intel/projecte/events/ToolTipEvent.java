package moze_intel.projecte.events;

import java.util.List;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.api.proxy.ISellProxy;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ToolTipEvent {

	/**
	 * Called from the tooltip callback registered on the client mod initializer.
	 */
	public static void onTooltip(List<Component> tooltip, ItemStack current, Player player, boolean shiftDown) {
		if (current.isEmpty()) {
			return;
		}
		if (ProjectEConfig.client.pedestalToolTips.get()) {
			IPedestalItem pedestalItem = PECapabilities.PEDESTAL_ITEM_CAPABILITY.find(current);
			if (pedestalItem != null) {
				tooltip.add(PELang.PEDESTAL_ON.translateColored(ChatFormatting.DARK_PURPLE));
				List<Component> description = pedestalItem.getPedestalDescription(player == null || player.level() == null ? 20 : player.level().tickRateManager().tickrate());
				if (description.isEmpty()) {
					tooltip.add(PELang.PEDESTAL_DISABLED.translateColored(ChatFormatting.RED));
				} else {
					tooltip.addAll(description);
				}
			}
		}

		if (ProjectEConfig.client.tagToolTips.get()) {
			//26.3 removed ItemStack#getTags; the tags hang off the item's holder now
			current.typeHolder().tags().forEach(tag -> tooltip.add(Component.literal("#" + tag.location())));
		}

		if (ProjectEConfig.client.emcToolTips.get() && (!ProjectEConfig.client.shiftEmcToolTips.get() || shiftDown)) {
			long value = IEMCProxy.INSTANCE.getValue(current);
			if (value > 0) {
				tooltip.add(EMCHelper.getEmcTextComponent(value, 1));
				if (current.getCount() > 1) {
					tooltip.add(EMCHelper.getEmcTextComponent(value, current.getCount()));
				}
				if (player != null && (!ProjectEConfig.client.shiftLearnedToolTips.get() || shiftDown)) {
					IKnowledgeProvider knowledgeProvider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
					if (knowledgeProvider != null && knowledgeProvider.hasKnowledge(current)) {
						tooltip.add(PELang.EMC_HAS_KNOWLEDGE.translateColored(ChatFormatting.YELLOW));
					} else {
						tooltip.add(PELang.EMC_NO_KNOWLEDGE.translateColored(ChatFormatting.RED));
					}
				}
			}
		}

		if (ISellProxy.INSTANCE.isUnsellable(current)) {
			tooltip.add(PELang.TOOLTIP_UNSELLABLE.translateColored(ChatFormatting.RED));
		}

		long value = current.getOrDefault(PEDataComponentTypes.STORED_EMC.get(), 0L);
		if (value == 0) {
			IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(current);
			if (emcHolder != null) {
				value = emcHolder.getStoredEmc(current);
			}
		}
		if (value > 0) {
			tooltip.add(PELang.EMC_STORED.translateColored(ChatFormatting.YELLOW, ChatFormatting.WHITE, EMCHelper.formatEmc(value)));
		}
	}
}
