package moze_intel.projecte.api.capabilities;

import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class PECapabilities {

	private PECapabilities() {
	}

	private static Identifier rl(String path) {
		return Identifier.fromNamespaceAndPath(ProjectEAPI.PROJECTE_MODID, path);
	}

	/**
	 * The capability object for IEmcStorage
	 */
	public static final PEBlockCapability<IEmcStorage, @Nullable Direction> EMC_STORAGE_CAPABILITY = PEBlockCapability.create(rl("emc_storage"), IEmcStorage.class, Direction.class);

	/**
	 * The capability object for IAlchBagProvider
	 */
	public static final PEEntityCapability<IAlchBagProvider> ALCH_BAG_CAPABILITY = PEEntityCapability.create(rl("alchemical_bag"), IAlchBagProvider.class);

	/**
	 * The capability object for IKnowledgeProvider
	 */
	public static final PEEntityCapability<IKnowledgeProvider> KNOWLEDGE_CAPABILITY = PEEntityCapability.create(rl("knowledge"), IKnowledgeProvider.class);

	/**
	 * The capability object for IAlchBagItem
	 */
	public static final PEItemCapability<IAlchBagItem> ALCH_BAG_ITEM_CAPABILITY = PEItemCapability.create(rl("alchemical_bag"), IAlchBagItem.class);

	/**
	 * The capability object for IAlchChestItem
	 */
	public static final PEItemCapability<IAlchChestItem> ALCH_CHEST_ITEM_CAPABILITY = PEItemCapability.create(rl("alchemical_chest"), IAlchChestItem.class);

	/**
	 * The capability object for IExtraFunction
	 */
	public static final PEItemCapability<IExtraFunction> EXTRA_FUNCTION_ITEM_CAPABILITY = PEItemCapability.create(rl("extra_function"), IExtraFunction.class);

	/**
	 * The capability object for IItemCharge
	 */
	public static final PEItemCapability<IItemCharge> CHARGE_ITEM_CAPABILITY = PEItemCapability.create(rl("charge"), IItemCharge.class);

	/**
	 * The capability object for IItemEmcHolder
	 */
	public static final PEItemCapability<IItemEmcHolder> EMC_HOLDER_ITEM_CAPABILITY = PEItemCapability.create(rl("emc_holder"), IItemEmcHolder.class);

	/**
	 * The capability object for IModeChanger
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final PEItemCapability<IModeChanger<?>> MODE_CHANGER_ITEM_CAPABILITY = PEItemCapability.create(rl("mode_changer"), (Class) IModeChanger.class);

	/**
	 * The capability object for IPedestalItem
	 */
	public static final PEItemCapability<IPedestalItem> PEDESTAL_ITEM_CAPABILITY = PEItemCapability.create(rl("pedestal"), IPedestalItem.class);

	/**
	 * The capability object for IProjectileShooter
	 */
	public static final PEItemCapability<IProjectileShooter> PROJECTILE_SHOOTER_ITEM_CAPABILITY = PEItemCapability.create(rl("projectile_shooter"), IProjectileShooter.class);
}
