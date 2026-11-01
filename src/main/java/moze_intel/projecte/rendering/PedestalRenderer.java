package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import moze_intel.projecte.gameObjs.block_entities.DMPedestalBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spins the item held on top of the dark matter pedestal.
 * <p>
 * 26.3 split block entity rendering into {@code extractRenderState} and {@code submit}, and removed the immediate-mode
 * {@code ItemRenderer#renderStatic}: an item is now resolved into an {@link ItemStackRenderState} during extraction and
 * submitted during the draw pass.
 */
public class PedestalRenderer implements BlockEntityRenderer<DMPedestalBlockEntity, PedestalRenderer.PEDisplayRenderState> {

	public static class PEDisplayRenderState extends BlockEntityRenderState {

		public final ItemStackRenderState item = new ItemStackRenderState();
		/**
		 * Rotation in degrees derived from the world clock, so the item spins at a steady rate.
		 */
		public float angle;
	}

	private final ItemModelResolver itemModelResolver;

	public PedestalRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public PEDisplayRenderState createRenderState() {
		return new PEDisplayRenderState();
	}

	@Override
	public void extractRenderState(@NotNull DMPedestalBlockEntity pedestal, @NotNull PEDisplayRenderState state, float partialTick,
			@Nullable Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(pedestal, state, partialTick, cameraPosition, crumblingOverlay);
		//A pedestal has no item entity to anchor the render state to, so the stack is resolved against the world instead.
		ItemStack stack = pedestal.getInventory().getStackInSlot(0);
		itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, pedestal.getLevel(), null, 0);
		long time = pedestal.getLevel() != null ? pedestal.getLevel().getGameTime() : 0;
		state.angle = (time % 80) / 80.0F * 360.0F;
	}

	@Override
	public void submit(@NotNull PEDisplayRenderState state, @NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
		if (state.item.isEmpty()) {
			return;
		}
		pose.pushPose();
		pose.translate(0.5, 1.0, 0.5);
		pose.rotate(Axis.YP.rotationDegrees(state.angle));
		pose.scale(0.6F, 0.6F, 0.6F);
		state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}
}
