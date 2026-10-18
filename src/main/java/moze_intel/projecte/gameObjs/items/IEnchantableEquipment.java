package moze_intel.projecte.gameObjs.items;

/**
 * Marks the player facing gear of ours that is enchantable like vanilla equipment even though it is not durability based.
 * <p>
 * Vanilla only lets an item be enchanted if {@link net.minecraft.world.item.Item#isEnchantable} allows it, whose default implementation requires a
 * {@code MAX_DAMAGE} component. Our equipment replaces that with the charge and stored EMC components, so each of our gear classes opts in explicitly and
 * implements this marker.
 * <p>
 * The marker is also what lets us keep the durability related enchantments off our gear: they are all built around losing durability, which our gear never does,
 * so they would be dead weight in the enchanting table.
 */
public interface IEnchantableEquipment {

	/**
	 * @return Whether the given stack may be enchanted, which aside from being unstackable always is the case for our equipment.
	 */
	default boolean isEnchantableEquipment(net.minecraft.world.item.ItemStack stack) {
		return stack.getMaxStackSize() == 1;
	}
}
