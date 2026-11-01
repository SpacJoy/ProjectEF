package moze_intel.projecte.gameObjs.block_entities;

import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.gameObjs.container.AlchChestContainer;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.BiFunction;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AlchBlockEntityChest extends EmcChestBlockEntity implements PEWorldlyContainer {

	public static final BiFunction<AlchBlockEntityChest, @Nullable Direction, IItemHandler> INVENTORY_PROVIDER = (chest, side) -> chest.inventory;

	private final StackHandler inventory = new StackHandler(104) {
		@Override
		public void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			if (level != null && !level.isClientSide()) {
				inventoryChanged = true;
			}
		}
	};
	private boolean inventoryChanged;

	public AlchBlockEntityChest(BlockPos pos, BlockState state) {
		super(PEBlockEntityTypes.ALCHEMICAL_CHEST, pos, state, 1_000);
	}

	@Override
	public void loadAdditional(@NotNull ValueInput input) {
		super.loadAdditional(input);
		input.child("inventory").ifPresent(inventory::deserialize);
	}

	@Override
	protected void saveAdditional(@NotNull ValueOutput output) {
		super.saveAdditional(output);
		inventory.serialize(output.child("inventory"));
	}

	public static void tickClient(Level level, BlockPos pos, BlockState state, AlchBlockEntityChest alchChest) {
		for (int i = 0, slots = alchChest.inventory.getSlots(); i < slots; i++) {
			ItemStack stack = alchChest.inventory.getStackInSlot(i);
			IAlchChestItem alchChestItem = PECapabilities.ALCH_CHEST_ITEM_CAPABILITY.find(stack);
			if (alchChestItem != null) {
				alchChestItem.updateInAlchChest(level, pos, stack);
			}
		}
		EmcChestBlockEntity.lidAnimateTick(level, pos, state, alchChest);
	}

	public static void tickServer(Level level, BlockPos pos, BlockState state, AlchBlockEntityChest alchChest) {
		StackHandler inventory = alchChest.inventory;
		for (int i = 0, slots = inventory.getSlots(); i < slots; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			IAlchChestItem alchChestItem = PECapabilities.ALCH_CHEST_ITEM_CAPABILITY.find(stack);
			if (alchChestItem != null && alchChestItem.updateInAlchChest(level, pos, stack)) {
				inventory.onContentsChanged(i);
			}
		}
		if (alchChest.inventoryChanged) {
			//If the inventory changed, resync so that the client can tick things properly
			alchChest.inventoryChanged = false;
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
		alchChest.updateComparators(level, pos);
	}

	public IItemHandler getInventory() {
		return inventory;
	}

	@NotNull
	@Override
	public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player playerIn) {
		return new AlchChestContainer(windowId, playerInventory, this);
	}

	@NotNull
	@Override
	public Component getDisplayName() {
		return TextComponentUtil.build(PEBlocks.ALCHEMICAL_CHEST);
	}


	@Override
	public IItemHandler getFullHandler() { return this.inventory; }

	@Override
	public IItemHandler getSideHandler(@org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) { return this.inventory; }
}
