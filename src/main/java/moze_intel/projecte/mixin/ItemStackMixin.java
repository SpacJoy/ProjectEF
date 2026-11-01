package moze_intel.projecte.mixin;

import java.util.function.BiConsumer;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the conditional (stack state dependent) attribute modifiers of ProjectEF items, replacing the item attribute modifier event that used to provide this.
 * <p>
 * 26.3 widened the group overload to a {@link TriConsumer} that also receives the display data, while the single slot overload kept its
 * {@link BiConsumer}, so the two injections no longer share a callback shape.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("TAIL"))
	private void projecte$conditionalAttributesByGroup(EquipmentSlotGroup slotGroup,
			TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> action, CallbackInfo ci) {
		ItemStack self = (ItemStack) (Object) this;
		if (self.getItem() instanceof IHasConditionalAttributes conditional) {
			conditional.adjustAttributes(self, (attribute, modifier, group) -> {
				if (group == slotGroup) {
					action.accept(attribute, modifier, ItemAttributeModifiers.Display.attributeModifiers());
				}
			});
		}
	}

	@Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
	private void projecte$conditionalAttributesBySlot(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> action, CallbackInfo ci) {
		ItemStack self = (ItemStack) (Object) this;
		if (self.getItem() instanceof IHasConditionalAttributes conditional) {
			conditional.adjustAttributes(self, (attribute, modifier, group) -> {
				if (group.test(slot)) {
					action.accept(attribute, modifier);
				}
			});
		}
	}
}
