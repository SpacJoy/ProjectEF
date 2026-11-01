package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.registration.PEPropertyIds;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stamps the registry id onto the properties of every block that is being registered.
 * <p>
 * 26.3 made {@code Properties#id} mandatory: the constructor reads it for the loot table and the description id, and
 * {@code effectiveDrops()} throws {@code Block id not set} when it is missing. The injection sits at the head of the
 * constructor so it runs before either field is read, and the handler has to be static because mixin refuses an
 * instance handler ahead of the {@code super()} call.
 */
@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

	@Inject(method = "<init>", at = @At("HEAD"))
	private static void projecte$setBlockId(BlockBehaviour.Properties properties, CallbackInfo ci) {
		PEPropertyIds.block(properties);
	}
}
