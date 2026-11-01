package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import moze_intel.projecte.gameObjs.block_entities.EmcChestBlockEntity;
import moze_intel.projecte.gameObjs.registration.impl.BlockRegistryObject;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//Only used on the client
// [VanillaCopy] Adapted from ChestRenderer
public class ChestRenderer implements BlockEntityRenderer<EmcChestBlockEntity, PEChestRenderState> {

	/**
	 * 26.3 needs an explicit {@link UvMapping} when a model part is submitted directly. The baked chest model already
	 * carries texture coordinates in the 0..1 range (vanilla only needs a mapping because it offsets into an atlas
	 * sprite), and the ProjectE chests bind their own texture, so the identity mapping is what the old raw
	 * {@code ModelPart#render(PoseStack, VertexConsumer, int, int)} call used implicitly.
	 */
	private static final UvMapping CHEST_UV = new UvMapping() {
		@Override
		public float getU(float u) {
			return u;
		}

		@Override
		public float getV(float v) {
			return v;
		}
	};

	private final ModelPart lid;
	private final ModelPart bottom;
	private final ModelPart lock;

	private final BlockRegistryObject<?, ?> type;
	private final Identifier texture;

	public ChestRenderer(BlockEntityRendererProvider.Context context, Identifier texture, BlockRegistryObject<?, ?> type) {
		this.texture = texture;
		this.type = type;
		ModelPart modelpart = context.bakeLayer(ModelLayers.CHEST);
		this.bottom = modelpart.getChild("bottom");
		this.lid = modelpart.getChild("lid");
		this.lock = modelpart.getChild("lock");
	}

	@Override
	public PEChestRenderState createRenderState() {
		return new PEChestRenderState();
	}

	@Override
	public void extractRenderState(@NotNull EmcChestBlockEntity chest, @NotNull PEChestRenderState state, float partialTick,
			@Nullable Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(chest, state, partialTick, cameraPosition, crumblingOverlay);
		//26.3 moved everything the submit pass needs into the render state; the block entity is only readable here.
		state.facingYRot = 0;
		if (chest.getLevel() != null && !chest.isRemoved()) {
			BlockState blockState = chest.getLevel().getBlockState(chest.getBlockPos());
			if (blockState.is(type.getBlock())) {
				state.facingYRot = -blockState.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();
			}
		}
		float openNess = 1.0F - chest.getOpenNess(partialTick);
		state.lidAngle = 1.0F - openNess * openNess * openNess;
	}

	@Override
	public void submit(@NotNull PEChestRenderState state, @NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
		pose.pushPose();
		if (state.facingYRot != 0) {
			pose.translate(0.5D, 0.5D, 0.5D);
			pose.rotate(Axis.YP.rotationDegrees(state.facingYRot));
			pose.translate(-0.5D, -0.5D, -0.5D);
		}
		//26.3: model parts are submitted rather than written straight into a buffer, so the pose is applied here and the
		//part rotation is only meaningful for the duration of this call.
		lid.xRot = -(state.lidAngle * Mth.HALF_PI);
		lock.xRot = lid.xRot;
		collector.submitModelPart(lid, pose, RenderTypes.entityCutout(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, CHEST_UV);
		collector.submitModelPart(lock, pose, RenderTypes.entityCutout(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, CHEST_UV);
		collector.submitModelPart(bottom, pose, RenderTypes.entityCutout(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, CHEST_UV);
		pose.popPose();
	}
}
