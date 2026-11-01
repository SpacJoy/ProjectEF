package moze_intel.projecte.mixin;

import moze_intel.projecte.gameObjs.blocks.ProjectETNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no {@code TntBlock.onCaughtFire} hook, so vanilla ignition paths that are not virtual
 * methods on the block call the static {@link TntBlock#prime} directly and spawn a plain vanilla
 * {@code PrimedTnt} instead of the custom ProjectEF entity. The most notable one is the vanilla
 * flint and steel dispenser behavior, which primes the block <em>before</em> removing it.
 *
 * <p>Every such caller still has the block present at that point, so this catches them generically
 * (including third party callers) by checking the block at the position. The fire spread path in
 * {@code FireBlock.checkBurnOut} removes the block <em>before</em> priming, so the state is
 * already gone by then and it stays handled by {@link FireBlockMixin} instead; its fallback call to
 * {@link TntBlock#prime} reads back air here and correctly falls through to vanilla behavior.
 *
 * <p>26.3 renamed {@code TntBlock#explode(Level, BlockPos)} to {@code prime(Level, BlockPos)} and made it
 * report whether anything was primed, so the hook now returns {@code true} to mean "handled".
 */
@Mixin(TntBlock.class)
public abstract class TntBlockMixin {

	@Inject(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private static void projecte$primeCustomTnt(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (level.getBlockState(pos).getBlock() instanceof ProjectETNT tnt) {
			if (!level.isClientSide()) {
				tnt.createAndAddEntity(level, pos, null);
			}
			cir.setReturnValue(true);
		}
	}
}
