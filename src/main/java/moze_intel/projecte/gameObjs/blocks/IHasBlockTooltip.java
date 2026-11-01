package moze_intel.projecte.gameObjs.blocks;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * Lets a block contribute lines to the tooltip of the item that places it.
 * <p>
 * 26.3 dropped {@code BlockBehaviour#appendHoverText} (and {@code BlockItem} no longer overrides it either): tooltip text
 * is an item concern only. The lines therefore have to travel through the {@code BlockItem}, which asks its block via
 * this interface — see {@link moze_intel.projecte.gameObjs.items.TooltipBlockItem}.
 */
public interface IHasBlockTooltip {

	/**
	 * Accepts the lines this block wants appended to its item's tooltip.
	 */
	void addTooltip(Consumer<Component> tooltip);
}
