package moze_intel.projecte.gameObjs.registration.impl;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.gameObjs.items.ICapabilityAware;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class ItemDeferredRegister extends PEDeferredRegister<Item> {

	public ItemDeferredRegister(String modid) {
		super(Registries.ITEM, modid, ItemRegistryObject::new);
	}

	@Override
	public void register() {
		super.register();
		registerCapabilities();
	}

	private void registerCapabilities() {
		for (PEDeferredHolder<Item, ? extends Item> entry : getEntries()) {
			Item item = entry.value();
			if (item instanceof IAlchBagItem) {
				PECapabilities.ALCH_BAG_ITEM_CAPABILITY.registerForItems((stack, context) -> (IAlchBagItem) stack.getItem(), item);
			}
			if (item instanceof IAlchChestItem) {
				PECapabilities.ALCH_CHEST_ITEM_CAPABILITY.registerForItems((stack, context) -> (IAlchChestItem) stack.getItem(), item);
			}
			if (item instanceof IExtraFunction) {
				PECapabilities.EXTRA_FUNCTION_ITEM_CAPABILITY.registerForItems((stack, context) -> (IExtraFunction) stack.getItem(), item);
			}
			if (item instanceof IItemCharge) {
				PECapabilities.CHARGE_ITEM_CAPABILITY.registerForItems((stack, context) -> (IItemCharge) stack.getItem(), item);
			}
			if (item instanceof IItemEmcHolder) {
				PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.registerForItems((stack, context) -> (IItemEmcHolder) stack.getItem(), item);
			}
			if (item instanceof IModeChanger<?>) {
				PECapabilities.MODE_CHANGER_ITEM_CAPABILITY.registerForItems((stack, context) -> (IModeChanger<?>) stack.getItem(), item);
			}
			if (item instanceof IPedestalItem) {
				PECapabilities.PEDESTAL_ITEM_CAPABILITY.registerForItems((stack, context) -> (IPedestalItem) stack.getItem(), item);
			}
			if (item instanceof IProjectileShooter) {
				PECapabilities.PROJECTILE_SHOOTER_ITEM_CAPABILITY.registerForItems((stack, context) -> (IProjectileShooter) stack.getItem(), item);
			}
			if (item instanceof ICapabilityAware capabilityAware) {
				capabilityAware.attachCapabilities();
			}
		}
	}

	public ItemRegistryObject<Item> register(String name) {
		return registerSimple(name, Item::new);
	}

	public ItemRegistryObject<Item> registerFireImmune(String name) {
		return registerFireImmune(name, Item::new);
	}

	public <ITEM extends Item> ItemRegistryObject<ITEM> registerSimple(String name, Function<Item.Properties, ITEM> sup) {
		return register(name, sup, UnaryOperator.identity());
	}

	public <ITEM extends Item> ItemRegistryObject<ITEM> registerFireImmune(String name, Function<Item.Properties, ITEM> sup) {
		return register(name, sup, Item.Properties::fireResistant);
	}

	public <ITEM extends Item> ItemRegistryObject<ITEM> registerNoStack(String name, Function<Item.Properties, ITEM> sup) {
		return register(name, sup, properties -> properties.stacksTo(1));
	}

	public <ITEM extends Item> ItemRegistryObject<ITEM> registerNoStackFireImmune(String name, Function<Item.Properties, ITEM> sup) {
		return register(name, sup, properties -> properties.stacksTo(1).fireResistant());
	}

	public <ITEM extends Item> ItemRegistryObject<ITEM> registerTool(String name, Function<Item.Properties, ITEM> sup) {
		return register(name, () -> sup.apply(new NoDurabilityItemProperties().stacksTo(1).fireResistant()));
	}

	/**
	 * 26.3 removed {@code ArmorItem}: the material and slot now live on {@code Item.Properties#humanoidArmor}, which also
	 * applies the durability/attribute/repair components the old ArmorItem constructor used to add. The slot is handed to
	 * the factory as well so subclasses do not have to hardcode it a second time.
	 * <p>
	 * ProjectE gear is meant to be damage free, so the properties object swallows the durability write that
	 * {@code humanoidArmor} performs; otherwise every piece would end up with a MAX_DAMAGE component.
	 */
	public <ITEM extends Item> ItemRegistryObject<ITEM> registerArmor(String name, net.minecraft.world.item.equipment.ArmorMaterial material,
			net.minecraft.world.item.equipment.ArmorType type, java.util.function.BiFunction<net.minecraft.world.item.equipment.ArmorType, Item.Properties, ITEM> sup) {
		return register(name, () -> sup.apply(type, new NoDurabilityItemProperties().stacksTo(1).fireResistant().humanoidArmor(material, type)));
	}

	public <ITEM extends Item> ItemRegistryObject<ITEM> register(String name, Function<Item.Properties, ITEM> sup, UnaryOperator<Item.Properties> propertyModifier) {
		return register(name, () -> sup.apply(propertyModifier.apply(new Item.Properties())));
	}

	@NotNull
	@Override
	@SuppressWarnings("unchecked")
	public <ITEM extends Item> ItemRegistryObject<ITEM> register(@NotNull String name, @NotNull Supplier<? extends ITEM> sup) {
		return (ItemRegistryObject<ITEM>) super.register(name, sup);
	}

	private static class NoDurabilityItemProperties extends Item.Properties {

		@NotNull
		@Override
		public Item.Properties durability(int maxDamage) {
			//NO-OP super setting durability components
			return this;
		}
	}
}
