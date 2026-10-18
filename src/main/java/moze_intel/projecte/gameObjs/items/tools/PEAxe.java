package moze_intel.projecte.gameObjs.items.tools;

import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEAxe extends AxeItem implements IItemCharge, IBarHelper, IEnchantableEquipment {

	private final IMatterType matterType;
	private final int numCharges;

	public PEAxe(IMatterType matterType, int numCharges, Properties props) {
		super(matterType, props.attributes(createAttributes(matterType, 5, -3))
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
		BlockState blockState = level.getBlockState(context.getClickedPos());
		//Order that it attempts to use the item:
		// Strip logs, AOE remove logs
		return ToolHelper.performActions(context, blockState, ToolHelper.stripLogsAOE(context, blockState, 0),
				(ctx, state) -> ToolHelper.scrapeAOE(ctx, state, 0),
				(ctx, state) -> ToolHelper.waxOffAOE(ctx, state, 0),
				(ctx, state) -> {
					if (state.is(BlockTags.LOGS)) {
						//Mass clear
						//Note: We already tried to strip the log in an earlier action
						return ToolHelper.clearTagAOE(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getItemInHand(), 0, BlockTags.LOGS);
					}
					return InteractionResult.PASS;
				});
	}
}