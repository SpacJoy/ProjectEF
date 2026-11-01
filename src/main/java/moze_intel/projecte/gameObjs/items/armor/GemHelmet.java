package moze_intel.projecte.gameObjs.items.armor;

import java.util.List;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class GemHelmet extends GemArmorBase {

	private static final boolean NIGHT_VISION_DEFAULT = false;

	public GemHelmet(Properties props) {
		super(ArmorType.HELMET, props.component(PEDataComponentTypes.NIGHT_VISION.get(), NIGHT_VISION_DEFAULT));
	}

	public static void toggleNightVision(ItemStack helm, Player player) {
		boolean oldValue = hasNightVision(helm);
		helm.set(PEDataComponentTypes.NIGHT_VISION.get(), !oldValue);
		player.sendSystemMessage(getComponent(!oldValue));
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		tooltip.accept(PELang.GEM_LORE_HELM.translate());
		tooltip.accept(PELang.NIGHT_VISION_PROMPT.translate(ClientKeyHelper.getKeyName(PEKeybind.HELMET_TOGGLE)));
		tooltip.accept(getComponent(hasNightVision(stack)));
	}

	private static boolean hasNightVision(ItemStack stack) {
		return stack.getOrDefault(PEDataComponentTypes.NIGHT_VISION.get(), NIGHT_VISION_DEFAULT);
	}

	private static Component getComponent(boolean nightVision) {
		if (nightVision) {
			return PELang.NIGHT_VISION.translate(ChatFormatting.GREEN, PELang.GEM_ENABLED);
		}
		return PELang.NIGHT_VISION.translate(ChatFormatting.RED, PELang.GEM_DISABLED);
	}

	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull net.minecraft.server.level.ServerLevel level, @NotNull Entity entity, @NotNull net.minecraft.world.entity.EquipmentSlot slot) {
		super.inventoryTick(stack, level, entity, slot);
		if (!level.isClientSide() && entity instanceof Player player && isEquipped(stack, player)) {
			if (PlayerHelper.checkHealCooldown(player)) {
				player.heal(2.0F);
			}

			if (hasNightVision(stack)) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 11 * SharedConstants.TICKS_PER_SECOND, 0, true, false));
			} else {
				player.removeEffect(MobEffects.NIGHT_VISION);
			}
		}
	}

	public static void doZap(Player player) {
		if (ProjectEConfig.server.difficulty.offensiveAbilities.get()) {
			BlockHitResult strikeResult = PlayerHelper.getBlockLookingAt(player, 120.0F);
			if (strikeResult.getType() != HitResult.Type.MISS) {
				BlockPos strikePos = strikeResult.getBlockPos();
				Level level = player.level();
				//26.3 moved the entity type constants onto EntityTypes
				LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
				if (lightning != null) {
					//26.3 renamed Entity#moveTo to snapTo
					lightning.snapTo(Vec3.atCenterOf(strikePos));
					lightning.setCause((ServerPlayer) player);
					level.addFreshEntity(lightning);
				}
			}
		}
	}
}
