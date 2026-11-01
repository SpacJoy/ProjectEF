package moze_intel.projecte.gameObjs.items;

import java.util.Objects;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import moze_intel.projecte.utils.OpenScreenHelper;
import moze_intel.projecte.api.item_handlers.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class AlchemicalBag extends ItemPE {

	public final DyeColor color;

	public AlchemicalBag(Properties props, DyeColor color) {
		super(props);
		this.color = color;
	}

	@NotNull
	@Override
	public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		if (!level.isClientSide()) {
			OpenScreenHelper.openMenuWithData(player, new ContainerProvider(player.getItemInHand(hand), hand), buf -> {
				buf.writeEnum(hand);
				buf.writeByte(player.getInventory().getSelectedSlot());
				buf.writeBoolean(false);
			});
		}

		return InteractionResult.SUCCESS;
	}

	public static ItemStack getFirstBagWithSuctionItem(Player player, NonNullList<ItemStack> inventory) {
		IAlchBagProvider alchBagProvider = null;
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && stack.getItem() instanceof AlchemicalBag bag) {
				if (alchBagProvider == null) {
					alchBagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player);
					if (alchBagProvider == null) {
						//If the player really doesn't have the capability, and it isn't just not loaded yet, exit
						break;
					}
				}
				IItemHandler inv = alchBagProvider.getBag(bag.color);
				for (int i = 0; i < inv.getSlots(); i++) {
					ItemStack ring = inv.getStackInSlot(i);
					if (!ring.isEmpty() && (ring.is(PEItems.BLACK_HOLE_BAND.get()) || ring.is(PEItems.VOID_RING.get()))) {
						if (ring.getOrDefault(PEDataComponentTypes.ACTIVE.get(), false)) {
							return stack;
						}
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}

	private class ContainerProvider implements MenuProvider {

		private final ItemStack stack;
		private final InteractionHand hand;

		private ContainerProvider(ItemStack stack, InteractionHand hand) {
			this.stack = stack;
			this.hand = hand;
		}

		@NotNull
		@Override
		public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
			IItemHandlerModifiable inv = (IItemHandlerModifiable) Objects.requireNonNull(PECapabilities.ALCH_BAG_CAPABILITY.find(player)).getBag(color);
			return new AlchBagContainer(windowId, playerInventory, hand, inv, playerInventory.getSelectedSlot(), false);
		}

		@NotNull
		@Override
		public Component getDisplayName() {
			return stack.getHoverName();
		}
	}
}
