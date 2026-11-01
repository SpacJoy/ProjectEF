package moze_intel.projecte.gameObjs.items.armor;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.List;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class GemLegs extends GemArmorBase {

	private static final Vec3 DOWNWARD_MOVEMENT = new Vec3(0, -0.32F, 0);

	public GemLegs(Properties props) {
		super(ArmorType.LEGGINGS, props);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		tooltip.accept(PELang.GEM_LORE_LEGS.translate());
	}

	private final Int2LongMap lastJumpTracker = new Int2LongOpenHashMap();

	private boolean jumpedRecently(Player player) {
		return lastJumpTracker.containsKey(player.getId()) && player.level().getGameTime() - lastJumpTracker.get(player.getId()) < 5;
	}

	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull net.minecraft.server.level.ServerLevel level, @NotNull Entity entity, @NotNull net.minecraft.world.entity.EquipmentSlot slot) {
		super.inventoryTick(stack, level, entity, slot);
		if (entity instanceof Player player && isEquipped(stack, player)) {
			if (level.isClientSide()) {
				//Fabric has no LivingJumpEvent, so approximate the jump moment client side as the first tick the player rises while airborne
				if (!player.onGround() && player.getDeltaMovement().y() > 0 && !jumpedRecently(player)) {
					lastJumpTracker.put(player.getId(), level.getGameTime());
				}
				if (player.isSecondaryUseActive() && !player.onGround() && player.getDeltaMovement().y() > -8 && !jumpedRecently(player)) {
					player.addDeltaMovement(DOWNWARD_MOVEMENT);
				}
			}
			if (player.isSecondaryUseActive()) {
				WorldHelper.repelEntitiesSWRG(level, player.getBoundingBox().inflate(3.5), player);
				if (!level.isClientSide() && player.getDeltaMovement().y() < -0.08) {
					for (Entity e : player.level().getEntities(player,
							player.getBoundingBox().move(player.getDeltaMovement()).inflate(2.0D),
							ent -> ent.isAlive() && ent.isPickable() && ent instanceof LivingEntity
					)) {
						e.hurt(level.damageSources().playerAttack(player), (float) -player.getDeltaMovement().y() * 6F);
					}
				}
			}
		}
	}
}
