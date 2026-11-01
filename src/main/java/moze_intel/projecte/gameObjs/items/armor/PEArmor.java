package moze_intel.projecte.gameObjs.items.armor;

import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

public abstract class PEArmor extends Item implements IEnchantableEquipment {

	private final ArmorType armorType;

	protected PEArmor(ArmorType armorType, Properties props) {
		//26.3 removed ArmorItem entirely: the material now travels through Item.Properties#humanoidArmor, so the caller
		// must have already applied it to props before handing them over.
		super(props);
		this.armorType = armorType;
	}

	/**
	 * Minimum percent damage will be reduced to if the full set is worn
	 */
	public abstract float getFullSetBaseReduction();

	/**
	 * Gets the max damage that a piece of this armor in a given slot can absorb of a specific type.
	 *
	 * @apiNote A value of zero means that there is no special bonus blocking powers for that damage type, and the piece's base reduction will be get used instead by the
	 * damage calculation event.
	 */
	public abstract float getMaxDamageAbsorb(ArmorType type, DamageSource source);

	public ArmorType getArmorType() {
		return armorType;
	}

	public ReductionInfo getReductionInfo(DamageSource source) {
		float maxDamageAbsorb = getMaxDamageAbsorb(armorType, source);
		float fullSetReduction = getFullSetBaseReduction();
		if (maxDamageAbsorb > 0 && fullSetReduction > 0) {
			float pieceEffectiveness = getPieceEffectiveness(armorType);
			return new ReductionInfo(fullSetReduction * pieceEffectiveness, maxDamageAbsorb * pieceEffectiveness);
		}
		return ReductionInfo.ZERO;
	}

	/**
	 * Gets the overall effectiveness of a given slots piece.
	 */
	public float getPieceEffectiveness(ArmorType type) {
		if (type == ArmorType.BOOTS || type == ArmorType.HELMET) {
			return 0.2F;
		} else if (type == ArmorType.CHESTPLATE || type == ArmorType.LEGGINGS) {
			return 0.3F;
		}
		return 0;
	}

	protected boolean isEquipped(ItemStack stack, Player player) {
		return player.getItemBySlot(armorType.getSlot()) == stack;
	}

	public record ReductionInfo(float percentReduced, float maxDamagedAbsorbed) {

		public static final ReductionInfo ZERO = new ReductionInfo(0, 0);

		public ReductionInfo add(ReductionInfo other) {
			if (this == ZERO) {
				return other;
			} else if (other == ZERO) {
				return this;
			}
			return new ReductionInfo(percentReduced + other.percentReduced, maxDamagedAbsorbed + other.maxDamagedAbsorbed);
		}
	}
}
