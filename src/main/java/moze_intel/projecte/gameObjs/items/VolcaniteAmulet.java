package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntityLavaProjectile;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
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
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.ChatFormatting;
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

public class VolcaniteAmulet extends ItemPE implements IProjectileShooter, IPedestalItem, IFireProtector, ICapabilityAware {

	public VolcaniteAmulet(Properties props) {
		super(props.component(PEDataComponentTypes.STORED_EMC.get(), 0L));
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
		ItemStack stack = ctx.getItemInHand();
		if (!level.isClientSide() && PlayerHelper.hasEditPermission(player, level, pos)) {
			if (!consumeFuel(player, stack, 32, true)) {
				player.sendSystemMessage(PELang.NOT_ENOUGH_EMC.translate());
				return InteractionResult.FAIL;
			}
			Direction sideHit = ctx.getClickedFace();
			Storage<FluidVariant> fluidStorage = WorldHelper.getCapability(level, FluidStorage.SIDED, pos, sideHit);
			if (fluidStorage != null) {
				try (Transaction tx = Transaction.openOuter()) {
					fluidStorage.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx);
					tx.commit();
				}
				return InteractionResult.CONSUME;
			}
			WorldHelper.placeFluid(player, level, pos, sideHit, Fluids.LAVA, false);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.TRANSMUTE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
	}

	@Override
	public boolean shootProjectile(@NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.TRANSMUTE.get(), SoundSource.PLAYERS, 1, 1);
		EntityLavaProjectile ent = new EntityLavaProjectile(player, player.level());
		ent.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5F, 1);
		player.level().addFreshEntity(ent);
		return true;
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		tooltip.accept(PELang.TOOLTIP_VOLCANITE_1.translate(ClientKeyHelper.getKeyName(PEKeybind.FIRE_PROJECTILE)));
		tooltip.accept(PELang.TOOLTIP_VOLCANITE_2.translate());
		tooltip.accept(PELang.TOOLTIP_VOLCANITE_3.translate());
		tooltip.accept(PELang.TOOLTIP_VOLCANITE_4.translate());
	}

	@Override
	public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockPos pos,
			@NotNull PEDESTAL pedestal) {
		if (!level.isClientSide() && ProjectEConfig.server.cooldown.pedestal.volcanite.get() != -1) {
			if (pedestal.getActivityCooldown() == 0) {
				//26.3 moved the weather flags off LevelData and onto the per level WeatherData saved data.
				if (level instanceof ServerLevel serverLevel) {
					WeatherData worldInfo = serverLevel.getWeatherData();
					worldInfo.setRainTime(0);
					worldInfo.setThunderTime(0);
					worldInfo.setRaining(false);
					worldInfo.setThundering(false);
				}
				pedestal.setActivityCooldown(level, pos, ProjectEConfig.server.cooldown.pedestal.volcanite.get());
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
		if (ProjectEConfig.server.cooldown.pedestal.volcanite.get() != -1) {
			list.add(PELang.PEDESTAL_VOLCANITE_1.translateColored(ChatFormatting.BLUE));
			list.add(PELang.PEDESTAL_VOLCANITE_2.translateColored(ChatFormatting.BLUE, MathUtils.tickToSecFormatted(ProjectEConfig.server.cooldown.pedestal.volcanite.get(), tickRate)));
		}
		return list;
	}

	@Override
	public void attachCapabilities() {
		IntegrationHelper.registerCuriosCapability(this);
	}
}
