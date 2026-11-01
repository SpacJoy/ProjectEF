package moze_intel.projecte.gameObjs.items;

import java.util.Objects;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Range;

public class ItemPE extends Item {

	public ItemPE(Properties props) {
		super(props);
	}

	private static <MODE> boolean modeMatches(IModeChanger<MODE> modeChanger, ItemStack oldStack, ItemStack newStack) {
		return Objects.equals(modeChanger.getMode(oldStack), modeChanger.getMode(newStack));
	}

	@Range(from = 0, to = Long.MAX_VALUE)
	private static long getEmc(ItemStack stack) {
		return stack.getOrDefault(PEDataComponentTypes.STORED_EMC.get(), 0L);
	}

	public static void removeEmc(ItemStack stack, float amount) {
		removeEmc(stack, EMCHelper.removeFractionalEMC(stack, amount));
	}

	public static void removeEmc(ItemStack stack, @Range(from = 0, to = Long.MAX_VALUE) long amount) {
		if (amount > 0) {
			stack.update(PEDataComponentTypes.STORED_EMC.get(), 0L, amount, (emc, change) -> Math.max(emc - change, 0));
		}
	}

	public static boolean hasEmc(Player player, ItemStack stack, @Range(from = 0, to = Long.MAX_VALUE) long amount, boolean subUnitRemoval) {
		if (subUnitRemoval && getEmc(stack) > 0) {
			return true;
		}
		return consumeFuel(player, stack, amount, false);
	}

	/**
	 * If this method returns true (and shouldRemove is false), it can be assumed the stack has at least the given amount of emc stored.
	 */
	public static boolean consumeFuel(Player player, ItemStack stack, @Range(from = 0, to = Long.MAX_VALUE) long amount, boolean shouldRemove) {
		if (amount <= 0) {
			return true;
		}
		long current = getEmc(stack);
		boolean updateEmc = shouldRemove;
		if (current < amount) {
			long consume = EMCHelper.consumePlayerFuel(player, amount - current);
			if (consume == -1) {
				return false;
			}
			long spaceFor = Long.MAX_VALUE - current;
			if (consume > spaceFor) {
				if (shouldRemove) {
					//Remove the amount from consume, and mark we don't need to remove it
					consume -= amount;
					shouldRemove = false;
					//Check if we can now account for all that we consumed in the space we have
					if (consume > spaceFor) {//If it is still too large clamp it
						current = Long.MAX_VALUE;
					} else {//Otherwise, add it
						current += consume;
					}
				} else {//If we are not removing, just clamp the amount we have
					current = Long.MAX_VALUE;
				}
			} else {
				current += consume;
			}
			updateEmc = true;
		}
		if (shouldRemove) {
			//Note: Even if current < amount when we started, we will exit early if we were not able to consume enough emc to get us to have consume >= amount
			current -= amount;
		}
		if (updateEmc) {
			stack.set(PEDataComponentTypes.STORED_EMC.get(), current);
		}
		return true;
	}

	/**
	 * 26.3 changed {@code Item#inventoryTick} to report an {@link net.minecraft.world.entity.EquipmentSlot} instead of a
	 * raw inventory index, so "is this stack in the hotbar or off hand" becomes a slot comparison.
	 */
	public static boolean hotBarOrOffHand(net.minecraft.world.entity.EquipmentSlot slot) {
		return slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND;
	}
}