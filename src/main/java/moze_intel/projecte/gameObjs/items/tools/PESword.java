package moze_intel.projecte.gameObjs.items.tools;

import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes.AttributeCollector;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class PESword extends SwordItem implements IExtraFunction, IItemCharge, IBarHelper, IHasConditionalAttributes {

	private final IMatterType matterType;
	private final int numCharges;

	public PESword(IMatterType matterType, int numCharges, int damage, Properties props) {
		super(matterType, props.attributes(createAttributes(matterType, damage, -2.4F))
				.component(PEDataComponentTypes.CHARGE.get(), 0)
				.component(PEDataComponentTypes.STORED_EMC.get(), 0L)
		);
		this.matterType = matterType;
		this.numCharges = numCharges;
	}

	@Override
	public boolean isEnchantable(@NotNull ItemStack stack) {
		return ItemHelper.isEnchantableEquipment(stack);
	}

	@Override
	public boolean isBarVisible(@NotNull ItemStack stack) {
		return true;
	}

	@Override
	public float getWidthForBar(ItemStack stack) {
		return 1 - getChargePercent(stack);
	}

	@Override
	public int getBarWidth(@NotNull ItemStack stack) {
		return getScaledBarWidth(stack);
	}

	@Override
	public int getBarColor(@NotNull ItemStack stack) {
		return getColorForBar(stack);
	}

	@Override
	public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
		return ToolHelper.getDestroySpeed(super.getDestroySpeed(stack, state), matterType, getCharge(stack));
	}

	@Override
	public int getNumCharges(@NotNull ItemStack stack) {
		return numCharges;
	}

	@Override
	public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity damaged, @NotNull LivingEntity damager) {
		ToolHelper.attackWithCharge(stack, damaged, damager, 1.0F);
		return true;
	}

	@NotNull

	@Override
	public boolean doExtraFunction(@NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
		if (player.getAttackStrengthScale(0F) == 1) {
			ToolHelper.attackAOE(stack, player, slayAll(stack), (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE), 0, hand);
			PlayerHelper.resetCooldown(player);
			return true;
		}
		return false;
	}

	protected boolean slayAll(@NotNull ItemStack stack) {
		return false;
	}

	@Override
	public void adjustAttributes(ItemStack stack, AttributeCollector collector) {
		ToolHelper.applyChargeAttributes(stack, collector);
	}
}
