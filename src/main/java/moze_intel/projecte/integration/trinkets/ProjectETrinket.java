package moze_intel.projecte.integration.trinkets;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Shared {@link TrinketCallback} behavior registered for every ProjectE accessory. Its whole job is forwarding the
 * trinket tick to {@link net.minecraft.world.item.Item#inventoryTick}, which is what drives the accessories' passive
 * abilities. Everything else keeps the default callback behavior.
 * <p>
 * 26.3 note: Trinkets was rewritten by Patbox and moved from {@code dev.emi.trinkets.api.Trinket} (an interface
 * registered per item through {@code TrinketsApi#registerTrinket}) to {@link TrinketCallback}, a component-like
 * callback attached with {@link TrinketCallback#setCallback}. Same single hook, new type.
 */
public class ProjectETrinket implements TrinketCallback {

	/**
	 * Trinkets keeps worn stacks in its own container rather than the player's {@link Inventory}, so vanilla never
	 * calls {@link net.minecraft.world.item.Item#inventoryTick} for them. Forward the trinket tick to it so the
	 * accessories' passive abilities keep working while worn, matching the behavior they have in the hotbar.
	 * <p>
	 * {@link EquipmentSlot#OFFHAND} is passed as the slot because it satisfies
	 * {@link moze_intel.projecte.gameObjs.items.ItemPE#hotBarOrOffHand(EquipmentSlot)} while not being a valid hotbar
	 * slot. No ProjectE accessory uses the slot argument for anything besides that check.
	 * <p>
	 * 26.3 additionally made {@code Item#inventoryTick} server-only (it takes a {@link ServerLevel} instead of a
	 * {@link net.minecraft.world.level.Level}), and every ProjectE accessory already bailed out on the client, so the
	 * client branch is simply skipped now.
	 */
	@Override
	public void tick(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		//Worn stacks are live references, and mutating one (draining EMC, toggling active, repairing) does not by
		//itself sync to the client the way a vanilla inventory slot would. Writing the stack back through the slot
		//access is what flags it, replacing the old TrinketInventory#markUpdate call.
		ItemStack before = stack.copy();
		stack.getItem().inventoryTick(stack, level, entity, EquipmentSlot.OFFHAND);
		if (!ItemStack.matches(before, stack)) {
			slot.set(stack);
		}
	}
}
