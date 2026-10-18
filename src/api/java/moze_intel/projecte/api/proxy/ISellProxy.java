package moze_intel.projecte.api.proxy;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Queries about whether an item may be sold (converted into EMC) to ProjectEF's transmutation machinery.
 * <p>
 * Other mods that implement their own selling path should consult this before accepting a stack, so that items which are deliberately kept out of the transmutation
 * table stay out of every equivalent screen as well. ProjectEF already applies the same check to its own transmutation table, condensers and relays.
 */
public interface ISellProxy {

	/**
	 * The proxy for sell-related queries.
	 */
	ISellProxy INSTANCE = java.util.ServiceLoader.load(ISellProxy.class).findFirst()
			.orElseThrow(() -> new IllegalStateException("No valid ServiceImpl for ISellProxy found, ProjectEF may be absent, damaged, or outdated"));

	/**
	 * Checks whether the given stack is forbidden from being sold, which is the case when it carries the "unsellable" enchantment. Only that exact stack is affected,
	 * other stacks of the same item remain sellable.
	 * <p>
	 * Can be called on both sides.
	 *
	 * @param stack The stack that is about to be sold.
	 *
	 * @return Whether the stack must not be sold, and must therefore not be converted into EMC or knowledge.
	 */
	boolean isUnsellable(@NotNull ItemStack stack);
}
