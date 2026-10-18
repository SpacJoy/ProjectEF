package moze_intel.projecte.impl;

import moze_intel.projecte.api.proxy.ISellProxy;
import moze_intel.projecte.gameObjs.PETags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

public class SellProxyImpl implements ISellProxy {

	@Override
	public boolean isUnsellable(@NotNull ItemStack stack) {
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
