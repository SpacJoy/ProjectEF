package moze_intel.projecte.gameObjs.blocks;

import moze_intel.projecte.utils.OpenScreenHelper;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlockEntityTypes;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import moze_intel.projecte.api.item_handlers.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatterFurnace extends AbstractFurnaceBlock implements IMatterBlock, PEEntityBlock<DMFurnaceBlockEntity> {

	private final EnumMatterType matterType;

	public MatterFurnace(Properties props, EnumMatterType type) {
		super(props);
		this.matterType = type;
	}

	@Nullable
	@Override
	public BlockEntityTypeRegistryObject<? extends DMFurnaceBlockEntity> getType() {
		return matterType == EnumMatterType.RED_MATTER ? PEBlockEntityTypes.RED_MATTER_FURNACE : PEBlockEntityTypes.DARK_MATTER_FURNACE;
	}

	@Override
	protected void openContainer(Level level, @NotNull BlockPos pos, @NotNull Player player) {
		if (!level.isClientSide()) {
			DMFurnaceBlockEntity furnace = WorldHelper.getBlockEntity(DMFurnaceBlockEntity.class, level, pos, true);
			if (furnace != null) {
				OpenScreenHelper.openMenuAt(player, furnace, pos);
			}
		}
	}

	@Override
	@Deprecated
	protected void affectNeighborsAfterRemoval(BlockState state, @NotNull net.minecraft.server.level.ServerLevel level, @NotNull BlockPos pos, boolean isMoving) {
		//26.3 dropped the newState parameter from affectNeighborsAfterRemoval
		IItemHandler handler = WorldHelper.getItemHandler(level, pos, state, null, null);
		WorldHelper.dropInventory(handler, level, pos);
		super.affectNeighborsAfterRemoval(state, level, pos, isMoving);
	}

	@Override
	@Deprecated
	public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Direction direction) {
		return ItemHandlerHelper.calcRedstoneFromInventory(WorldHelper.getItemHandler(level, pos, state, null, null));
	}

	@Override
	public EnumMatterType getMatterType() {
		return matterType;
	}
}