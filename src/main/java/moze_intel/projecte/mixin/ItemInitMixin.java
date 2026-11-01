package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.registration.PEPropertyIds;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stamps the registry id onto the properties of every item that is being registered.
 * <p>
 * 26.3 made {@code Item.Properties#id} mandatory: the constructor reads it for the description id, the model and the
 * registry name, and {@code itemIdOrThrow()} throws {@code Item id not set} when it is missing. The injection sits at
 * the head of the constructor and the handler is static for the same reason as the block side.
 */
@Mixin(Item.class)
public class ItemInitMixin {

	@Inject(method = "<init>", at = @At("HEAD"))
	private static void projecte$setItemId(Item.Properties properties, CallbackInfo ci) {
		PEPropertyIds.item(properties);
	}
}
