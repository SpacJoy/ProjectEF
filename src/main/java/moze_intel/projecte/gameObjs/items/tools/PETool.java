package moze_intel.projecte.gameObjs.items.tools;

import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class PETool extends DiggerItem implements IItemCharge, IBarHelper, IEnchantableEquipment {

	protected final IMatterType matterType;
	private final int numCharges;

	public PETool(IMatterType matterType, TagKey<Block> blocks, int numCharges, Properties props) {
		super(matterType, blocks, props.component(PEDataComponentTypes.CHARGE.get(), 0)
				.component(PEDataComponentTypes.STORED_EMC.get(), 0L)
		);
		this.matterType = matterType;
		this.numCharges = numCharges;
	}


	@Override
	public int getEnchantmentValue() {
		return matterType.getEnchantmentValue();
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
		return ToolHelper.getDestroySpeed(getShortCutDestroySpeed(stack, state), matterType, getCharge(stack));
	}

	@Override
	public int getNumCharges(@NotNull ItemStack stack) {
		return numCharges;
	}

	/**
	 * Override this if we need to also include any "shortcuts" that specific vanilla tool types include for specific blocks/material types.
	 * <p>
	 * For example: Axes are "effective" on all types of wood, but do not automatically allow HARVESTING of all types of wood.
	 */
	protected float getShortCutDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
		return super.getDestroySpeed(stack, state);
	}
}