package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets our equipment be enchanted in an enchanting table.
 * <p>
 * {@link Item#isEnchantable} only allows items that are unstackable and carry a {@code MAX_DAMAGE} component, and our gear replaces durability with the charge and
 * stored EMC components, so the vanilla default rejects all of it and the enchanting table offers no options at all.
 * <p>
 * This is handled here rather than by overriding the method in every gear class, because an interface cannot override a method of its superclass: a
 * {@code default} method on {@link IEnchantableEquipment} named {@code isEnchantable} would be shadowed by {@link Item#isEnchantable} instead of replacing it. Going
 * through the marker interface also means a newly added piece of gear only has to implement it, instead of having to remember the override.
 */
@Mixin(Item.class)
public abstract class ItemMixin {

	@Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
	private void projecte$enchantableEquipment(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof IEnchantableEquipment equipment) {
			cir.setReturnValue(equipment.isEnchantableEquipment(stack));
		}
	}
}
