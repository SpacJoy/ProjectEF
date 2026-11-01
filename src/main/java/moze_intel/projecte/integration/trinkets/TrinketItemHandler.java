package moze_intel.projecte.integration.trinkets;

import eu.pb4.trinkets.api.TrinketInventory;
import java.util.List;
import moze_intel.projecte.api.item_handlers.IItemHandlerModifiable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes a player's Trinkets inventories as a ProjectE {@link IItemHandlerModifiable} so the loader-neutral
 * curio-polling helpers (fuel consumption, the Repair Talisman, hotbar-or-accessory ability checks) can see worn
 * accessories. Slots map one-to-one onto the underlying {@link TrinketInventory} containers, so the stacks returned by
 * {@link #getStackInSlot(int)} are the live worn stacks. Callers that mutate one in place must call
 * {@link #markSlotChanged(int)} afterwards, otherwise the change never reaches the client.
 * <p>
 * 26.3 note: {@code TrinketInventory} no longer declares {@code markUpdate()} (that method only exists on the
 * implementation class now). {@link TrinketInventory} is a plain {@link net.minecraft.world.Container}, and its
 * {@code setChanged()} is what flags the owning attachment to push a sync — so this class talks to the interface only.
 */
public class TrinketItemHandler implements IItemHandlerModifiable {

	private final TrinketInventory[] slotInventory;
	private final int[] slotLocalIndex;

	public TrinketItemHandler(List<TrinketInventory> inventories) {
		int size = 0;
		for (TrinketInventory inventory : inventories) {
			size += inventory.getContainerSize();
		}
		this.slotInventory = new TrinketInventory[size];
		this.slotLocalIndex = new int[size];
		int slot = 0;
		for (TrinketInventory inventory : inventories) {
			for (int local = 0, count = inventory.getContainerSize(); local < count; local++) {
				slotInventory[slot] = inventory;
				slotLocalIndex[slot] = local;
				slot++;
			}
		}
	}

	@Override
	public int getSlots() {
		return slotInventory.length;
	}

	@NotNull
	@Override
	public ItemStack getStackInSlot(int slot) {
		validateSlotIndex(slot);
		return slotInventory[slot].getItem(slotLocalIndex[slot]);
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		validateSlotIndex(slot);
		TrinketInventory inventory = slotInventory[slot];
		inventory.setItem(slotLocalIndex[slot], stack);
		inventory.setChanged();
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		} else if (!isItemValid(slot, stack)) {
			return stack;
		}
		validateSlotIndex(slot);
		TrinketInventory inventory = slotInventory[slot];
		int local = slotLocalIndex[slot];
		ItemStack existing = inventory.getItem(local);
		int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
		if (!existing.isEmpty()) {
			if (!ItemStack.isSameItemSameComponents(stack, existing)) {
				return stack;
			}
			limit -= existing.getCount();
		}
		if (limit <= 0) {
			return stack;
		}
		boolean reachedLimit = stack.getCount() > limit;
		if (!simulate) {
			if (existing.isEmpty()) {
				inventory.setItem(local, reachedLimit ? stack.copyWithCount(limit) : stack.copy());
			} else {
				existing.grow(reachedLimit ? limit : stack.getCount());
			}
			inventory.setChanged();
		}
		return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
	}

	@NotNull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount == 0) {
			return ItemStack.EMPTY;
		}
		validateSlotIndex(slot);
		TrinketInventory inventory = slotInventory[slot];
		int local = slotLocalIndex[slot];
		ItemStack existing = inventory.getItem(local);
		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}
		int toExtract = Math.min(amount, existing.getMaxStackSize());
		if (existing.getCount() <= toExtract) {
			if (!simulate) {
				inventory.setItem(local, ItemStack.EMPTY);
				inventory.setChanged();
			}
			return existing.copy();
		}
		if (!simulate) {
			inventory.setItem(local, existing.copyWithCount(existing.getCount() - toExtract));
			inventory.setChanged();
		}
		return existing.copyWithCount(toExtract);
	}

	@Override
	public int getSlotLimit(int slot) {
		//See ItemStackHandler#getSlotLimit: a fixed 99 would cap stacks that a stacking mod raised past 99
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return true;
	}

	@Override
	public void markSlotChanged(int slot) {
		validateSlotIndex(slot);
		//setChanged is what registers the inventory with its attachment's dirty set, which then gets drained into a sync
		// packet. Writing the stack back through setItem marks it too, but in-place mutations need this explicit call.
		slotInventory[slot].setChanged();
	}

	private void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= slotInventory.length) {
			throw new IndexOutOfBoundsException("Slot " + slot + " not in valid range - [0," + slotInventory.length + ")");
		}
	}
}
