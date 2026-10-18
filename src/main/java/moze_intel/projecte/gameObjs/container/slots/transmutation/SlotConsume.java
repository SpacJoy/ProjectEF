package moze_intel.projecte.gameObjs.container.slots.transmutation;

import java.math.BigInteger;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.InventoryContainerSlot;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.api.proxy.ISellProxy;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotConsume extends InventoryContainerSlot {

	private final TransmutationInventory inv;

	public SlotConsume(TransmutationInventory inv, int index, int x, int y) {
		super(inv, index, x, y);
		this.inv = inv;
	}

	@Override
	public void set(@NotNull ItemStack stack) {
		//Note: Guard against the unsellable enchantment here as well, as setting the slot contents directly (networking, other mods) bypasses mayPlace
		if (inv.isServer() && !stack.isEmpty() && !ISellProxy.INSTANCE.isUnsellable(stack)) {
			inv.handleKnowledge(stack);
			inv.addEmc(BigInteger.valueOf(IEMCProxy.INSTANCE.getSellValue(stack)).multiply(BigInteger.valueOf(stack.getCount())));
			this.setChanged();
		}
	}

	@Override
	public boolean mayPlace(@NotNull ItemStack stack) {
		//Note: Items carrying the unsellable enchantment stay in place even though they have an EMC value, as that is exactly what the enchantment is for
		if (ISellProxy.INSTANCE.isUnsellable(stack)) {
			return false;
		}
		return IEMCProxy.INSTANCE.hasValue(stack) || stack.is(PEItems.TOME_OF_KNOWLEDGE.get());
	}
}
