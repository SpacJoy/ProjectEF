package moze_intel.projecte.gameObjs.items;

import java.util.function.Consumer;
import moze_intel.projecte.gameObjs.blocks.IHasBlockTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link BlockItem} that forwards {@link IHasBlockTooltip#addTooltip} into the item tooltip.
 * <p>
 * 26.3 deleted {@code BlockBehaviour#appendHoverText}, so blocks can no longer inject tooltip lines themselves; the
 * block instead implements {@link IHasBlockTooltip} and this item relays those lines here.
 */
public class TooltipBlockItem extends BlockItem {

	public TooltipBlockItem(Block block, Item.Properties properties) {
		//26.3 block items default to the item.<ns>.<path> translation key, while the lang files only carry block.<ns>.<path>.
		// Vanilla Items#registerBlock flips the prefix the same way, so mirror it here instead of duplicating every key.
		super(block, properties.useBlockDescriptionPrefix());
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull TooltipDisplay display,
			@NotNull Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		if (getBlock() instanceof IHasBlockTooltip hasTooltip) {
			hasTooltip.addTooltip(tooltip);
		}
	}
}
