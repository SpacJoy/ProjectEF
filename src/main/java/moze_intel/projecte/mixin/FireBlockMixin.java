package moze_intel.projecte.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import moze_intel.projecte.gameObjs.blocks.ProjectETNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric has no {@code TntBlock.onCaughtFire} hook (NeoForge routes every ignition path through
 * it, which the NeoForge version of {@link ProjectETNT} relies on). In vanilla, fire spread that
 * reaches a TNT block is hard-coded in {@link FireBlock#checkBurnOut} to call the static
 * {@link TntBlock#explode}, which spawns a plain vanilla {@code PrimedTnt} instead of the custom
 * ProjectEF TNT entity. This mixin redirects that call so {@link ProjectETNT} blocks spawn their
 * custom entity.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

	/**
	 * Remembers the block {@link FireBlock#checkBurnOut} is about to burn, because by the time the
	 * redirected {@link TntBlock#explode} call below runs, the block has already been replaced by
	 * fire or removed, so the original block can no longer be read from the level.
	 */
	@Unique
	private static final Map<Level, Map<BlockPos, ProjectETNT>> BURNING_PROJECTE_TNT = new WeakHashMap<>();

	@Redirect(method = "checkBurnOut", at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private static BlockState projecte$captureTnt(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof ProjectETNT tnt) {
			BURNING_PROJECTE_TNT.computeIfAbsent(level, ignored -> new HashMap<>()).put(pos, tnt);
		}
		return state;
	}

	@Redirect(method = "checkBurnOut", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
	private static boolean projecte$igniteTnt(Level level, BlockPos pos) {
		//26.3 renamed TntBlock.explode(Level, BlockPos) to prime(Level, BlockPos) and made it return whether it worked.
		Map<BlockPos, ProjectETNT> burning = BURNING_PROJECTE_TNT.get(level);
		ProjectETNT tnt = burning == null ? null : burning.remove(pos);
		if (tnt != null) {
			tnt.createAndAddEntity(level, pos, null);
			return true;
		}
		return TntBlock.prime(level, pos);
	}
}
