package moze_intel.projecte.utils;

import moze_intel.projecte.gameObjs.PETags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

/**
 * Checks around selling items to the transmutation table.
 */
public final class SellHelper {

	private SellHelper() {
	}

	/**
	 * @return Whether the given stack carries an enchantment that forbids selling it, which stops it from being sold to the transmutation table even though it has an
	 * EMC value. Only that exact stack is affected, other stacks of the same item stay sellable.
	 * @implNote This is checked through an enchantment tag rather than a registry lookup, as selling is validated from code paths that have no level or registry
	 * access at all, such as the slot that accepts items into the transmutation table. The enchantment registry and its tags are synchronized to the client, so the
	 * check behaves the same on both sides.
	 */
	public static boolean isUnsellable(@NotNull ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		//Check both the enchantment component and the one that holds the enchantments of an enchanted book
		return EnchantmentHelper.hasTag(stack, PETags.Enchantments.UNSELLABLE) || hasTag(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
	}

	private static boolean hasTag(@NotNull ItemEnchantments enchantments) {
		if (enchantments.isEmpty()) {
			return false;
		}
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.is(PETags.Enchantments.UNSELLABLE)) {
				return true;
			}
		}
		return false;
	}
}
