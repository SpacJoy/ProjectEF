package moze_intel.projecte.gameObjs.items.tools;

import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEShovel extends Item implements IItemCharge, IBarHelper, IEnchantableEquipment {

	private final IMatterType matterType;
	private final int numCharges;

	/**
	 * 26.3 deleted {@code ShovelItem}: the flattern/douse behaviour is ours already, only the tool component needed
	 * rebuilding, which {@code Item.Properties#shovel} does from the matter's {@link net.minecraft.world.item.ToolMaterial}.
	 */
	public PEShovel(IMatterType matterType, int numCharges, Properties props) {
		super(matterType.applyShovel(props, 2, -3)
				.component(PEDataComponentTypes.CHARGE.get(), 0)
				.component(PEDataComponentTypes.STORED_EMC.get(), 0L)
		);
		this.matterType = matterType;
		this.numCharges = numCharges;
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

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState blockState = level.getBlockState(pos);
		return ToolHelper.performActions(context, blockState, ToolHelper.flattenAOE(context, blockState, 0),
				ToolHelper::dowseCampfire,
				(ctx, state) -> {
					if (state.is(PETags.Blocks.VEIN_SHOVEL)) {
						return ToolHelper.tryVeinMine(ctx.getPlayer(), ctx.getItemInHand(), ctx.getClickedPos(), ctx.getClickedFace());
					}
					return InteractionResult.PASS;
				}, (ctx, state) -> ToolHelper.digAOE(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getItemInHand(), ctx.getClickedPos(),
						ctx.getClickedFace(), false, 0));
	}
}
