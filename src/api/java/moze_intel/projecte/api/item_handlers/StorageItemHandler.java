package moze_intel.projecte.api.item_handlers;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts a fabric transfer api {@link Storage} of items to the {@link IItemHandler} interface, so that ProjectEF's inventory interactions can work against any
 * inventory another mod exposes. Slotted storages (which vanilla containers and most modded inventories are) map directly to slots; non slotted storages are
 * exposed as a single virtual slot.
 */
public class StorageItemHandler implements IItemHandler {

	private final Storage<ItemVariant> storage;

	public StorageItemHandler(Storage<ItemVariant> storage) {
		this.storage = storage;
	}

	public Storage<ItemVariant> getStorage() {
		return storage;
	}

	@Override
	public int getSlots() {
		if (storage instanceof SlottedStorage<ItemVariant> slotted) {
			return slotted.getSlotCount();
		}
		return 1;
	}

	@NotNull
	@Override
	public ItemStack getStackInSlot(int slot) {
		if (storage instanceof SlottedStorage<ItemVariant> slotted) {
			if (slot < 0 || slot >= slotted.getSlotCount()) {
				return ItemStack.EMPTY;
			}
			SingleSlotStorage<ItemVariant> view = slotted.getSlot(slot);
			if (view.isResourceBlank()) {
				return ItemStack.EMPTY;
			}
			return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
		}
		//Non slotted storages don't have stable slot contents, expose the first non empty view for slot zero
		if (slot == 0) {
			for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
				return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
			}
		}
		return ItemStack.EMPTY;
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if (stack.isEmpty() || !storage.supportsInsertion()) {
			return stack;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted;
			if (storage instanceof SlottedStorage<ItemVariant> slotted) {
				if (slot < 0 || slot >= slotted.getSlotCount()) {
					return stack;
				}
				inserted = slotted.getSlot(slot).insert(ItemVariant.of(stack), stack.getCount(), transaction);
			} else {
				inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
			}
			if (!simulate) {
				transaction.commit();
			}
			if (inserted >= stack.getCount()) {
				return ItemStack.EMPTY;
			}
			return stack.copyWithCount(stack.getCount() - (int) inserted);
		}
	}

	@NotNull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0 || !storage.supportsExtraction()) {
			return ItemStack.EMPTY;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			ItemVariant resource;
			long extracted;
			if (storage instanceof SlottedStorage<ItemVariant> slotted) {
				if (slot < 0 || slot >= slotted.getSlotCount()) {
					return ItemStack.EMPTY;
				}
				SingleSlotStorage<ItemVariant> view = slotted.getSlot(slot);
				if (view.isResourceBlank()) {
					return ItemStack.EMPTY;
				}
				resource = view.getResource();
				extracted = view.extract(resource, amount, transaction);
			} else {
				resource = null;
				extracted = 0;
				for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
					resource = view.getResource();
					extracted = view.extract(resource, amount, transaction);
					if (extracted > 0) {
						break;
					}
				}
			}
			if (resource == null || extracted <= 0) {
				return ItemStack.EMPTY;
			}
			if (!simulate) {
				transaction.commit();
			}
			return resource.toStack((int) extracted);
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		//See ItemStackHandler#getSlotLimit: 99 is vanilla's marker for "no per slot cap", not a real ceiling, and a
		// stacking mod that raises MAX_STACK_SIZE past 99 would otherwise be clamped right back down here.
		if (storage instanceof SlottedStorage<ItemVariant> slotted && slot >= 0 && slot < slotted.getSlotCount()) {
			return (int) Math.min(slotted.getSlot(slot).getCapacity(), Integer.MAX_VALUE);
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return true;
	}
}
