package moze_intel.projecte.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gem boots reduce the fov change from the speed boost, replacing the fov modifier event.
 * <p>
 * The subtraction has to track the boots' movement speed modifier: vanilla computes the fov modifier as
 * {@code (speed / walkingSpeed + 1) / 2}, so a +50% boost lands on 1.25 and cancelling it takes 0.25.
 * Keep this in sync with {@code GemFeet}'s {@code ADD_MULTIPLIED_TOTAL} amount.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

	private static final float GEM_BOOTS_FOV_CANCEL = 0.25F;

	@ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
	private float projecte$gemBootsFov(float original) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		ItemStack boots = self.getItemBySlot(EquipmentSlot.FEET);
		if (!boots.isEmpty() && boots.is(PEItems.GEM_BOOTS.get())) {
			return original - GEM_BOOTS_FOV_CANCEL * Minecraft.getInstance().options.fovEffectScale().get().floatValue();
		}
		return original;
	}
}
