package moze_intel.projecte.client;

import moze_intel.projecte.gameObjs.items.armor.GemFeet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Client half of the gem boots flight assist.
 * <p>
 * 26.3 changed {@code ItemStack#inventoryTick} so it only calls {@code Item#inventoryTick} when the level is a
 * {@code ServerLevel}, which means the client branch that used to live in {@link GemFeet} never ran. The jump assist
 * has to read {@code ClientInput}, so it belongs on the client tick instead of the item tick.
 */
@Environment(EnvType.CLIENT)
public final class GemFeetClientTick {

	private static final Vec3 VERTICAL_MOVEMENT = new Vec3(0, 0.1, 0);

	private GemFeetClientTick() {
	}

	public static void tick(@NotNull LocalPlayer player) {
		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
		if (!(boots.getItem() instanceof GemFeet)) {
			return;
		}
		boolean flying = player.getAbilities().flying;
		if (!flying && player.input.keyPresses.jump()) {
			player.addDeltaMovement(VERTICAL_MOVEMENT);
		}
		if (!player.onGround()) {
			Vec3 deltaMovement = player.getDeltaMovement();
			if (deltaMovement.y() <= 0) {
				//Slow the fall so the boots feel like a glide rather than a drop
				player.setDeltaMovement(deltaMovement = deltaMovement.multiply(1, 0.9, 1));
			}
			if (!flying) {
				if (player.zza < 0) {
					//Moving backwards is slower than forwards
					player.setDeltaMovement(deltaMovement.multiply(0.9, 1, 0.9));
				} else if (player.zza > 0 && deltaMovement.lengthSqr() < 3) {
					//Forward momentum builds up to a cap
					player.setDeltaMovement(deltaMovement.multiply(1.1, 1, 1.1));
				}
			}
		}
	}
}
