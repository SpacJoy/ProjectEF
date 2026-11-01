package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Renders a flat, always-camera-facing quad for the ProjectE projectiles.
 * <p>
 * 26.3 removed {@code MultiBufferSource} and the immediate-mode {@code render(...)} hook: an {@code EntityRenderer} is now
 * a {@code <T, S>} pair and draws through {@link SubmitNodeCollector}. The geometry that used to be written straight into
 * a buffer builder is handed to {@link SubmitNodeCollector#submitCustomGeometry} instead, which defers the actual draw
 * while keeping the same {@link VertexConsumer} API.
 */
public class EntitySpriteRenderer<ENTITY extends Entity> extends EntityRenderer<ENTITY, EntityRenderState> {

	private final Identifier texture;
	private final RenderType renderType;

	public EntitySpriteRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context);
		this.texture = texture;
		this.renderType = PERenderType.sprite(texture);
	}

	@Override
	protected int getBlockLightLevel(@NotNull ENTITY entity, @NotNull net.minecraft.core.BlockPos pos) {
		return 15;
	}

	/**
	 * 26.3 dropped {@code getTextureLocation}: the texture now travels through the render type instead of being resolved
	 * per frame, so this is only kept as the source of {@link #renderType}.
	 */
	public Identifier getTexture() {
		return texture;
	}

	@Override
	public void submit(@NotNull EntityRenderState state, @NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
		pose.pushPose();
		pose.scale(0.5F, 0.5F, 0.5F);
		//26.3 replaced entityRenderDispatcher.cameraOrientation() with the orientation carried by the camera render state.
		pose.rotate(camera.orientation);
		collector.submitCustomGeometry(pose, renderType, EntitySpriteRenderer::buildQuad);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	private static void buildQuad(PoseStack.Pose pose, VertexConsumer consumer) {
		int light = 0xF000F0;
		vertex(consumer, pose, light, 0, 0, 0, 1);
		vertex(consumer, pose, light, 1, 0, 1, 1);
		vertex(consumer, pose, light, 1, 1, 1, 0);
		vertex(consumer, pose, light, 0, 1, 0, 0);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int light, float x, int y, int u, int v) {
		consumer.addVertex(pose, x - 0.5F, y, 0)
				.setColor(-1)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0F, 0F, 1F);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}
}
