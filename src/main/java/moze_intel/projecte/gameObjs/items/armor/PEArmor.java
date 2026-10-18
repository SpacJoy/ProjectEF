package moze_intel.projecte.gameObjs.items.armor;

import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class PEArmor extends ArmorItem implements IEnchantableEquipment {

	protected PEArmor(Holder<ArmorMaterial> material, ArmorItem.Type armorPiece, Properties props) {
		super(material, armorPiece, props);
	}

	/**
	 * Our armor is not durability based (damage is absorbed through EMC), so the vanilla default implementation, which requires a {@code MAX_DAMAGE} component, would
	 * always reject it. Instead any of our armor pieces can be enchanted, just like vanilla gear.
	 */
	@Override
	public boolean isEnchantable(@NotNull ItemStack stack) {
		return isEnchantableEquipment(stack);
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
	public abstract float getMaxDamageAbsorb(ArmorItem.Type type, DamageSource source);

	public ReductionInfo getReductionInfo(DamageSource source) {
		float maxDamageAbsorb = getMaxDamageAbsorb(type, source);
		float fullSetReduction = getFullSetBaseReduction();
		if (maxDamageAbsorb > 0 && fullSetReduction > 0) {
			float pieceEffectiveness = getPieceEffectiveness(type);
			return new ReductionInfo(fullSetReduction * pieceEffectiveness, maxDamageAbsorb * pieceEffectiveness);
		}
		return ReductionInfo.ZERO;
	}

	/**
	 * Gets the overall effectiveness of a given slots piece.
	 */
	public float getPieceEffectiveness(ArmorItem.Type type) {
		if (type == ArmorItem.Type.BOOTS || type == ArmorItem.Type.HELMET) {
			return 0.2F;
		} else if (type == ArmorItem.Type.CHESTPLATE || type == ArmorItem.Type.LEGGINGS) {
			return 0.3F;
		}
		return 0;
	}

	protected boolean isEquipped(ItemStack stack, Player player) {
		return player.getItemBySlot(type.getSlot()) == stack;
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
