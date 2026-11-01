package moze_intel.projecte.gameObjs.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom TNT block for the Nova Catalyst / Nova Cataclysm.
 *
 * <p>The NeoForge version of this class relies on the NeoForge added {@code TntBlock.onCaughtFire}
 * hook, which routes every ignition path (flint and steel, fire spread, fire charges, burning
 * projectiles, ...) through one method. Fabric has no such hook, so every vanilla ignition path that
 * is not overridden here falls through to the static {@link TntBlock#explode} and spawns a plain
 * vanilla {@link PrimedTnt} instead of the custom ProjectEF entity. The overrides below replicate
 * the vanilla priming behavior while spawning the custom entity. The fire spread path is
 * hard-coded in {@code FireBlock.checkBurnOut} and is handled separately by
 * {@code moze_intel.projecte.mixin.FireBlockMixin}; {@code WorldHelper#igniteBlock} and the
 * dispenser behavior already create the custom entity via {@link #createAndAddEntity}.
 */
public class ProjectETNT extends TntBlock {

	private final TNTEntityCreator tntEntityCreator;

	public ProjectETNT(Properties properties, TNTEntityCreator tntEntityCreator) {
		super(properties);
		this.tntEntityCreator = tntEntityCreator;
	}

	public void createAndAddEntity(@NotNull Level level, @NotNull BlockPos pos, @Nullable LivingEntity igniter) {
		PrimedTnt tnt = tntEntityCreator.create(level, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F, igniter);
		level.addFreshEntity(tnt);
		level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
	}

	public DispenseItemBehavior createDispenseItemBehavior() {
		//[VanillaCopy] Based off vanilla's TNT behavior
		return new DefaultDispenseItemBehavior() {
			@NotNull
			@Override
			protected ItemStack execute(@NotNull BlockSource source, @NotNull ItemStack stack) {
				BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
				createAndAddEntity(source.level(), blockpos, null);
				source.level().gameEvent(null, GameEvent.ENTITY_PLACE, blockpos);
				stack.shrink(1);
				return stack;
			}
		};
	}

	@NotNull
	@Override
	protected InteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
			@NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
		if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
		} else {
			//[VanillaCopy] TntBlock.useItemOn, except we spawn the custom ProjectEF entity
			if (!level.isClientSide()) {
				createAndAddEntity(level, pos, player);
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
			if (stack.is(Items.FLINT_AND_STEEL)) {
				stack.hurtAndBreak(1, player, hand);
			} else {
				stack.consume(1, player);
			}
			player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
			return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
		}
	}

	@Override
	protected void onProjectileHit(@NotNull Level level, @NotNull BlockState state, @NotNull BlockHitResult hit, @NotNull Projectile projectile) {
		//[VanillaCopy] TntBlock.onProjectileHit, except we spawn the custom ProjectEF entity
		if (!level.isClientSide()) {
			BlockPos pos = hit.getBlockPos();
			Entity owner = projectile.getOwner();
			//26.3: Entity#mayInteract only accepts a ServerLevel
			if (level instanceof ServerLevel serverLevel && projectile.isOnFire() && projectile.mayInteract(serverLevel, pos)) {
				createAndAddEntity(level, pos, owner instanceof LivingEntity livingentity ? livingentity : null);
				level.removeBlock(pos, false);
			}
		}
	}

	@Override
	protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock,
			@NotNull Orientation orientation, boolean movedByPiston) {
		//[VanillaCopy] TntBlock.neighborChanged, except we spawn the custom ProjectEF entity
		if (level.hasNeighborSignal(pos)) {
			if (!level.isClientSide()) {
				createAndAddEntity(level, pos, null);
			}
			level.removeBlock(pos, false);
		}
	}

	@Override
	protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean movedByPiston) {
		//[VanillaCopy] TntBlock.onPlace, except we spawn the custom ProjectEF entity
		if (!oldState.is(state.getBlock())) {
			if (level.hasNeighborSignal(pos)) {
				if (!level.isClientSide()) {
					createAndAddEntity(level, pos, null);
				}
				level.removeBlock(pos, false);
			}
		}
	}

	@NotNull
	@Override
	public BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
		//[VanillaCopy] TntBlock.playerWillDestroy, except we spawn the custom ProjectEF entity
		if (!level.isClientSide() && !player.isCreative() && state.getValue(UNSTABLE)) {
			createAndAddEntity(level, pos, null);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	//26.3: Block#wasExploded only runs on the server, so it takes a ServerLevel directly
	public void wasExploded(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull Explosion explosion) {
		PrimedTnt tnt = tntEntityCreator.create(level, (float) pos.getX() + 0.5F, pos.getY(), (float) pos.getZ() + 0.5F, explosion.getIndirectSourceEntity());
		int fuse = tnt.getFuse();
		tnt.setFuse((short) (level.getRandom().nextInt(fuse / 4) + fuse / 8));
		level.addFreshEntity(tnt);
	}

	@FunctionalInterface
	public interface TNTEntityCreator {

		PrimedTnt create(Level level, double posX, double posY, double posZ, @Nullable LivingEntity igniter);
	}
}
