package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import org.jetbrains.annotations.NotNull;

public abstract class BlockDirection extends Block {

	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

	public BlockDirection(Properties props) {
		super(props);
	}

	@Override
	protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> props) {
		super.createBlockStateDefinition(props);
		props.add(FACING);
	}

	@NotNull
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	@Deprecated
	protected void affectNeighborsAfterRemoval(@NotNull BlockState state, @NotNull net.minecraft.server.level.ServerLevel level, @NotNull BlockPos pos, boolean isMoving) {
		//26.3 dropped the newState parameter from affectNeighborsAfterRemoval, so there is nothing to compare against
		IItemHandler handler = WorldHelper.getItemHandler(level, pos, state, null, null);
		WorldHelper.dropInventory(handler, level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
	}

	@Override
	@Deprecated
	public void attack(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player) {
		if (!level.isClientSide()) {
			ItemStack stack = player.getMainHandItem();
			if (!stack.isEmpty() && stack.is(PEItems.PHILOSOPHERS_STONE.get())) {
				level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(FACING, player.getDirection().getOpposite()));
			}
		}
	}

	@NotNull
	@Override
	@Deprecated
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@NotNull
	@Override
	@Deprecated
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
}
