package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets our equipment be enchanted in an enchanting table.
 * <p>
 * Vanilla {@code ItemStack#isEnchantable} only accepts stacks that carry an {@code ENCHANTABLE} component and no enchantments yet, and our gear replaces
 * durability with the charge and stored EMC components, so the vanilla default rejects all of it and the enchanting table offers no options at all.
 * <p>
 * This is handled here rather than by overriding the method in every gear class, because an interface cannot override a method of its superclass: a
 * {@code default} method on {@link IEnchantableEquipment} named {@code isEnchantable} would be shadowed by the item's own method instead of replacing it.
 * Going through the marker interface also means a newly added piece of gear only has to implement it, instead of having to remember the override.
 * <p>
 * 26.3 moved {@code isEnchantable} from {@code Item} onto {@code ItemStack} (it now inspects the stack's components), so the mixin target moved with it.
 */
@Mixin(ItemStack.class)
public abstract class ItemMixin {

	@Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
	private void projecte$enchantableEquipment(CallbackInfoReturnable<Boolean> cir) {
		ItemStack self = (ItemStack) (Object) this;
		if (self.getItem() instanceof IEnchantableEquipment equipment) {
			cir.setReturnValue(equipment.isEnchantableEquipment(self));
		}
	}
}
