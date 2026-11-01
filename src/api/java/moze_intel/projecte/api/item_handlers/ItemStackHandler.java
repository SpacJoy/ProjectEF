package moze_intel.projecte.api.item_handlers;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

/**
 * A simple {@link IItemHandlerModifiable} implementation backed by a {@link NonNullList} of stacks.
 */
public class ItemStackHandler implements IItemHandlerModifiable {

	protected NonNullList<ItemStack> stacks;

	public ItemStackHandler() {
		this(1);
	}

	public ItemStackHandler(int size) {
		this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
	}

	public ItemStackHandler(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	public void setSize(int size) {
		this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
	}

	/**
	 * Exposes the backing list of this handler for serialization purposes. Do not modify the size of the returned list.
	 */
	public NonNullList<ItemStack> getStacks() {
		return stacks;
	}

	@Override
	public int getSlots() {
		return stacks.size();
	}

	@NotNull
	@Override
	public ItemStack getStackInSlot(int slot) {
		validateSlotIndex(slot);
		return stacks.get(slot);
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		validateSlotIndex(slot);
		stacks.set(slot, stack);
		onContentsChanged(slot);
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
		ItemStack existing = stacks.get(slot);
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
				stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack.copy());
			} else {
				existing.grow(reachedLimit ? limit : stack.getCount());
			}
			onContentsChanged(slot);
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
		ItemStack existing = stacks.get(slot);
		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}
		int toExtract = Math.min(amount, existing.getMaxStackSize());
		if (existing.getCount() <= toExtract) {
			if (!simulate) {
				stacks.set(slot, ItemStack.EMPTY);
				onContentsChanged(slot);
			}
			return existing.copy();
		}
		if (!simulate) {
			stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
			onContentsChanged(slot);
		}
		return existing.copyWithCount(toExtract);
	}

	@Override
	public int getSlotLimit(int slot) {
		//This is a per slot cap, and insertItem takes the minimum of it and the stack's own limit. Vanilla Container
		// reports 99 here as its "no per slot cap" marker, but 99 is a real number to Math.min, so a stack raised past
		// 99 by a stacking mod (Stackable and friends rewrite the MAX_STACK_SIZE component) would be silently capped
		// back down. Reporting the unbounded value leaves the item's own limit in charge.
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return true;
	}

	/**
	 * Bridges the public in-place mutation signal onto this class' existing {@link #onContentsChanged(int)} hook, so that
	 * subclasses reacting to content changes (marking a block entity dirty, recalculating EMC, ...) also see mutations
	 * that bypassed {@link #insertItem(int, ItemStack, boolean)} / {@link #extractItem(int, int, boolean)}.
	 */
	@Override
	public void markSlotChanged(int slot) {
		validateSlotIndex(slot);
		onContentsChanged(slot);
	}

	// 26.3: block entities persist through ValueOutput/ValueInput instead of raw CompoundTags, so a
	// handler writes itself into a child node and reads back through ContainerHelper's codec based lists.
	public void serialize(ValueOutput output) {
		ContainerHelper.saveAllItems(output, stacks);
	}

	public void deserialize(ValueInput input) {
		ContainerHelper.loadAllItems(input, stacks);
		onLoad();
	}

	public CompoundTag serializeNBT(HolderLookup.Provider provider) {
		// 26.3: ItemStack no longer has save(Provider, CompoundTag); it is codec-only now, so every
		// stack is encoded through ItemStack.CODEC against a registry-aware NBT ops instance.
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		ListTag nbtTagList = new ListTag();
		for (int slot = 0; slot < stacks.size(); slot++) {
			ItemStack stack = stacks.get(slot);
			if (!stack.isEmpty()) {
				Optional<Tag> encoded = ItemStack.CODEC.encodeStart(ops, stack).result();
				if (encoded.isEmpty()) {
					continue;
				}
				CompoundTag itemTag = (CompoundTag) encoded.get();
				itemTag.putInt("Slot", slot);
				nbtTagList.add(itemTag);
			}
		}
		CompoundTag nbt = new CompoundTag();
		nbt.put("Items", nbtTagList);
		nbt.putInt("Size", stacks.size());
		return nbt;
	}

	public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
		setSize(nbt.contains("Size") ? nbt.getIntOr("Size", stacks.size()) : stacks.size());
		ListTag tagList = nbt.getListOrEmpty("Items");
		for (int i = 0; i < tagList.size(); i++) {
			Optional<CompoundTag> optionalItemTags = tagList.getCompound(i);
			if (optionalItemTags.isEmpty()) {
				continue;
			}
			CompoundTag itemTags = optionalItemTags.get();
			int slot = itemTags.getIntOr("Slot", -1);
			if (slot >= 0 && slot < stacks.size()) {
				ItemStack.CODEC.parse(ops, itemTags).result().ifPresent(stack -> stacks.set(slot, stack));
			}
		}
		onLoad();
	}

	protected void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= stacks.size()) {
			throw new IndexOutOfBoundsException("Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
		}
	}

	protected void onLoad() {
	}

	protected void onContentsChanged(int slot) {
	}
}
