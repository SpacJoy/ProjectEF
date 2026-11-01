package moze_intel.projecte.gameObjs.items.tools;

import java.util.List;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IEnchantableEquipment;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class PEShears extends ShearsItem implements IItemCharge, IBarHelper, IEnchantableEquipment {

	private final IMatterType matterType;
	private final int numCharges;

	public PEShears(IMatterType matterType, int numCharges, Properties props) {
		super(props.component(PEDataComponentTypes.CHARGE.get(), 0)
						.component(PEDataComponentTypes.STORED_EMC.get(), 0L)
						.component(DataComponents.TOOL, new Tool(List.of(
								Tool.Rule.minesAndDrops(blocks(PETags.Blocks.MINEABLE_WITH_PE_SHEARS), matterType.getSpeed()),
								Tool.Rule.overrideSpeed(blocks(BlockTags.LEAVES), 15.0F),
								Tool.Rule.overrideSpeed(blocks(BlockTags.WOOL), 5.0F),
								//26.3: Tool.Rule only accepts HolderSet now, so the two-block case has to be built explicitly.
								Tool.Rule.overrideSpeed(HolderSet.direct(Blocks.VINE.builtInRegistryHolder(), Blocks.GLOW_LICHEN.builtInRegistryHolder()), 2.0F)
						), 1.0F, 1, true))
		);
		this.matterType = matterType;
		this.numCharges = numCharges;
	}

	/**
	 * 26.3 made every {@code Tool.Rule} factory take a {@link HolderSet} instead of a {@link TagKey}. The bootstrap lookup
	 * is the same one vanilla's {@code ToolMaterial#applyToolProperties} uses, so block tags are resolved the same way.
	 */
	private static HolderSet<Block> blocks(TagKey<Block> tag) {
		return BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK).getOrThrow(tag);
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
	public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		return ToolHelper.shearEntityAOE(player, hand, 0);
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null) {
			Level level = context.getLevel();
			BlockState state = level.getBlockState(context.getClickedPos());
			if (state.is(BlockTags.LEAVES)) {
				//Mass clear leaves
				ToolHelper.clearTagAOE(level, player, context.getHand(), context.getItemInHand(), 0, BlockTags.LEAVES);
			}
		}
		return InteractionResult.PASS;
	}
}
