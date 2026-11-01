package moze_intel.projecte.api.item_handlers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes only the main (hotbar + storage) portion of a player inventory, excluding armor and offhand slots.
 */
public class PlayerMainInvWrapper extends RangedWrapper {

	private final Inventory inventoryPlayer;

	public PlayerMainInvWrapper(Inventory inv) {
		super(new ContainerItemHandler(inv), 0, inv.getNonEquipmentItems().size());
		inventoryPlayer = inv;
	}

	public Inventory getInventoryPlayer() {
		return inventoryPlayer;
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		ItemStack rest = super.insertItem(slot, stack, simulate);
		if (rest.getCount() != stack.getCount()) {
			//The inserted item got picked up "by the player", trigger the pickup animation on a matching stack
			ItemStack inSlot = getStackInSlot(slot);
			if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
				inSlot.setPopTime(5);
			}
		}
		return rest;
	}
}
