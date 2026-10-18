package moze_intel.projecte.mixin;

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
}
