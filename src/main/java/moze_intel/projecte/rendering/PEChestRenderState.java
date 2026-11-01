package moze_intel.projecte.rendering;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Snapshot of everything {@link ChestRenderer} needs to draw a ProjectE chest.
 * <p>
 * 26.3 split block entity rendering into an extraction pass ({@code extractRenderState}) and a submission pass
 * ({@code submit}). The submission pass no longer receives the {@link net.minecraft.world.level.block.entity.BlockEntity},
 * so the facing rotation and the animated lid angle have to be captured here first.
 */
public class PEChestRenderState extends BlockEntityRenderState {

	/**
	 * Rotation of the chest around the Y axis in degrees, taken from the block's {@code HORIZONTAL_FACING}.
	 */
	public float facingYRot;
	/**
	 * The eased lid opening amount, 0 when closed and 1 when fully open.
	 */
	public float lidAngle;
}
