package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntityWaterProjectile;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.WeatherData;
import org.jetbrains.annotations.NotNull;

public class EvertideAmulet extends ItemPE implements IProjectileShooter, IPedestalItem, ICapabilityAware {

	public EvertideAmulet(Properties props) {
		super(props);
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null) {
			return InteractionResult.FAIL;
		}
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		if (!level.isClientSide() && PlayerHelper.hasEditPermission(player, level, pos)) {
			Direction sideHit = ctx.getClickedFace();
			Storage<FluidVariant> fluidStorage = WorldHelper.getCapability(level, FluidStorage.SIDED, pos, sideHit);
			if (fluidStorage != null) {
				try (Transaction tx = Transaction.openOuter()) {
					fluidStorage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
					tx.commit();
				}
				return InteractionResult.CONSUME;
			}
			WorldHelper.placeFluid(player, level, pos, sideHit, Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
			level.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.WATER_MAGIC.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
	}

	@Override
	public boolean shootProjectile(@NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
		Level level = player.level();
		if (ProjectEConfig.server.items.opEvertide.get() || !level.environmentAttributes().getDimensionValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES)) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.WATER_MAGIC.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			EntityWaterProjectile ent = new EntityWaterProjectile(player, level);
			ent.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1);
			level.addFreshEntity(ent);
			return true;
		}
		return false;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		tooltip.accept(PELang.TOOLTIP_EVERTIDE_1.translate(ClientKeyHelper.getKeyName(PEKeybind.FIRE_PROJECTILE)));
		tooltip.accept(PELang.TOOLTIP_EVERTIDE_2.translate());
		tooltip.accept(PELang.TOOLTIP_EVERTIDE_3.translate());
		tooltip.accept(PELang.TOOLTIP_EVERTIDE_4.translate());
	}

	@Override
	public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockPos pos,
			@NotNull PEDESTAL pedestal) {
		if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.evertide.get() != -1) {
			if (pedestal.getActivityCooldown() == 0) {
				//26.3 moved the weather flags off LevelData and onto the per level WeatherData saved data.
				if (level instanceof ServerLevel serverLevel) {
					WeatherData worldInfo = serverLevel.getWeatherData();
					int i = (300 + level.getRandom().nextInt(600)) * SharedConstants.TICKS_PER_SECOND;
					worldInfo.setRainTime(i);
					worldInfo.setThunderTime(i);
					worldInfo.setRaining(true);
				}
				pedestal.setActivityCooldown(level, pos, ProjectEConfig.server.cooldown.pedestal.evertide.get());
			} else {
				pedestal.decrementActivityCooldown(level, pos);
			}
		}
		return false;
	}

	@NotNull
	@Override
	public List<Component> getPedestalDescription(float tickRate) {
		List<Component> list = new ArrayList<>();
		if (ProjectEConfig.server.cooldown.pedestal.evertide.get() != -1) {
			list.add(PELang.PEDESTAL_EVERTIDE_1.translateColored(ChatFormatting.BLUE));
			list.add(PELang.PEDESTAL_EVERTIDE_2.translateColored(ChatFormatting.BLUE, MathUtils.tickToSecFormatted(ProjectEConfig.server.cooldown.pedestal.evertide.get(), tickRate)));
		}
		return list;
	}

	@Override
	public void attachCapabilities() {
		FluidStorage.ITEM.registerForItems((stack, context) -> new InfiniteWaterStorage(), this);
		IntegrationHelper.registerCuriosCapability(this);
	}

	/**
	 * Fabric Transfer API Storage exposing infinite, extraction-only water.
	 */
	private static class InfiniteWaterStorage implements Storage<FluidVariant> {

		//Large but non-overflowing amount to advertise for the single view
		private static final long AMOUNT = FluidConstants.BUCKET * 1000L;

		@Override
		public boolean supportsInsertion() {
			return false;
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			return resource.isOf(Fluids.WATER) ? maxAmount : 0;
		}

		@NotNull
		@Override
		public Iterator<StorageView<FluidVariant>> iterator() {
			return java.util.Collections.<StorageView<FluidVariant>>singletonList(new WaterView()).iterator();
		}

		private static class WaterView implements StorageView<FluidVariant> {

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return resource.isOf(Fluids.WATER) ? maxAmount : 0;
			}

			@Override
			public boolean isResourceBlank() {
				return false;
			}

			@NotNull
			@Override
			public FluidVariant getResource() {
				return FluidVariant.of(Fluids.WATER);
			}

			@Override
			public long getAmount() {
				return AMOUNT;
			}

			@Override
			public long getCapacity() {
				return AMOUNT;
			}
		}
	}
}
