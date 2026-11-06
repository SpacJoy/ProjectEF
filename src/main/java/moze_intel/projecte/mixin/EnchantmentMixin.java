package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the durability based enchantments off our equipment.
 * <p>
 * Our gear has no durability at all (the charge and stored EMC components take that role), so {@code unbreaking} and {@code mending} would be dead weight in the
 * enchanting table: they do nothing on an item that never takes damage, and mending additionally competes for the XP a player would otherwise keep. Vanilla only
 * decides which enchantments apply to an item through the enchantment tags, and because our gear is in the vanilla equipment tags ({@code #minecraft:swords} and
 * friends) it is dragged into {@code #minecraft:enchantable/durability} as well. Tags are additive, so there is no way to opt an item out of one; the check below
 * is the equivalent of opting out.
 * <p>
 * The check lives on the enchantment rather than on our items, so it also covers the anvil and the {@code /enchant} command, which both go through
 * {@link Enchantment#canEnchant(ItemStack)}. It only ever applies to items that opted into {@link IEnchantableEquipment}, so gear from other mods is untouched.
 * <p>
 * The second injection works the other way around: it opens {@code canEnchant} up for the unsellable enchantment, whose whole purpose is being applied to items
 * the enchanting table would never offer it for. See {@link #projecte$allowUnsellableOnAnyItem(ItemStack, CallbackInfoReturnable)}.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

	@Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
	private void projecte$skipDurabilityEnchantments(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Enchantment self = (Enchantment) (Object) this;
		if (self.definition().supportedItems().unwrapKey().filter(ItemTags.DURABILITY_ENCHANTABLE::equals).isPresent()
			&& stack.getItem() instanceof IEnchantableEquipment) {
			//The enchantment is only offered for durability based gear, and this item of ours never loses durability
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void projecte$skipDurabilityEnchantmentsOnAnvil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Enchantment self = (Enchantment) (Object) this;
		if (self.definition().supportedItems().unwrapKey().filter(ItemTags.DURABILITY_ENCHANTABLE::equals).isPresent()
			&& stack.getItem() instanceof IEnchantableEquipment) {
			//Also keep the anvil and /enchant from applying it, as they bypass isSupportedItem
			cir.setReturnValue(false);
		}
	}

	/**
	 * Lets the unsellable enchantment be applied to any item through its enchanted book.
	 * <p>
	 * The enchantment is deliberately not obtainable from the enchanting table: its {@code supported_items} only lists equipment, and that list is what the table
	 * picks candidates from. But vanilla reuses the very same list for {@link Enchantment#canEnchant(ItemStack)}, which is the only check the anvil makes before it
	 * copies an enchantment out of a book onto the target item. A book of it would therefore be rejected for everything outside that list, leaving the crafting
	 * recipe with no use: marking an arbitrary stack as unsellable is the entire point of the enchantment, so the anvil and the {@code /enchant} command accept any
	 * target here.
	 * <p>
	 * Only {@code canEnchant} is overridden, not {@code isSupportedItem}, because the enchanting table filters its candidates through
	 * {@link Enchantment#isPrimaryItem(ItemStack)} instead, so the table keeps offering the enchantment for exactly the equipment it did before. Loot tables that
	 * use an {@code enchant_randomly} function in its "compatible" mode are affected the same way the anvil is, but only if a pack lists this enchantment there
	 * explicitly.
	 */
	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void projecte$allowUnsellableOnAnyItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Enchantment self = (Enchantment) (Object) this;
		if (self.definition().supportedItems().unwrapKey().filter(PETags.Items.ENCHANTABLE_UNSELLABLE::equals).isPresent()) {
			//The unsellable enchantment is meant to be applied through its enchanted book, so accept any target
			cir.setReturnValue(true);
		}
	}
}
