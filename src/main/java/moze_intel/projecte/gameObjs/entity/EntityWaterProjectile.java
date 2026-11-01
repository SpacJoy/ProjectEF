package moze_intel.projecte.gameObjs.entity;

import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class EntityWaterProjectile extends NoGravityThrowableProjectile {

	public EntityWaterProjectile(EntityType<EntityWaterProjectile> type, Level level) {
		super(type, level);
	}

	public EntityWaterProjectile(Player entity, Level level) {
		super(PEEntityTypes.WATER_PROJECTILE.get(), entity, level);
	}

	@Override
	protected void onInsideBlock(@NotNull BlockState state) {
		if (!level().isClientSide() && state.getFluidState().is(FluidTags.WATER)) {
			discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		Level level = level();
		if (!level.isClientSide() && isAlive()) {
			if (getOwner() instanceof Player player) {
				for (BlockPos pos : WorldHelper.positionsAround(blockPosition(), 3)) {
					BlockState state = level.getBlockState(pos);
					FluidState fluidState = state.getFluidState();
					if (fluidState.is(FluidTags.LAVA)) {
						pos = pos.immutable();
						if (state.getBlock() instanceof LiquidBlock) {
							//If it is a source block convert it
							Block block = fluidState.isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
							PlayerHelper.checkedPlaceBlock(player, level, pos, block.defaultBlockState());
						} else {
							//Otherwise if it is lava logged, "void" the lava as we can't place a block in that spot
							WorldHelper.drainFluid(player, level, pos, state);
						}
						playSound(SoundEvents.GENERIC_BURN, 0.5F, 2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);
					}
				}
			}
			if (getY() > level.getMaxY()) {
				//26.3 moved the weather flags off LevelData and onto the per level WeatherData saved data.
				if (level instanceof ServerLevel serverLevel) {
					serverLevel.getWeatherData().setRaining(true);
				}
				discard();
			}
		}
	}

	@Override
	protected void onHitBlock(@NotNull BlockHitResult result) {
		super.onHitBlock(result);
		if (!level().isClientSide() && getOwner() instanceof Player player) {
			WorldHelper.placeFluid(player, level(), result.getBlockPos(), result.getDirection(), Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
		}
	}

	@Override
	protected void onHitEntity(@NotNull EntityHitResult result) {
		super.onHitEntity(result);
		if (!level().isClientSide()) {
			Entity ent = result.getEntity();
			if (ent.isOnFire()) {
				ent.clearFire();
			}
			ent.push(getDeltaMovement().scale(2));
		}
	}
}