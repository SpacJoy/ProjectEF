package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.UUID;
import moze_intel.projecte.PECore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the Yue / Clar heart circle decal on top of a player's body.
 * <p>
 * 26.3 turned render layers into a state-driven pass: {@link RenderLayer} is now {@code <S extends EntityRenderState, M>}
 * and only receives a {@code submit(PoseStack, SubmitNodeCollector, int, S, float, float)} call, without the live
 * {@link Player}. The two hardcoded UUIDs are therefore resolved through {@link AvatarRenderState#id}, which is the only
 * thing the state carries that points back at the entity.
 */
public class LayerYue extends RenderLayer<AvatarRenderState, PlayerModel> {

	private static final UUID SIN_UUID = UUID.fromString("5f86012c-ca4b-451a-989c-8fab167af647");
	private static final UUID CLAR_UUID = UUID.fromString("e5c59746-9cf7-4940-a849-d09e1f1efc13");
	private static final Identifier HEART_LOC = PECore.rl("textures/models/heartcircle.png");
	private static final Identifier YUE_LOC = PECore.rl("textures/models/yuecircle.png");

	public LayerYue(AvatarRenderer<?> renderer) {
		super(renderer);
	}

	@Override
	public void submit(@NotNull PoseStack pose, @NotNull SubmitNodeCollector collector, int light, @NotNull AvatarRenderState state, float limbSwing, float limbSwingAmount) {
		if (state.isInvisible) {
			return;
		}
		Identifier texture = getTextureLocation(state);
		if (texture == null) {
			return;
		}
		pose.pushPose();
		getParentModel().body.translateAndRotate(pose);
		double yShift = -0.498;
		if (state.isCrouching) {
			pose.rotate(Axis.XN.rotation(0.5F));
			yShift = -0.44;
		}
		pose.rotate(Axis.ZP.rotation(Mth.PI));
		pose.scale(3, 3, 3);
		pose.translate(-0.5, yShift, -0.5);
		collector.submitCustomGeometry(pose, PERenderType.glowingSprite(texture), LayerYue::buildQuad);
		pose.popPose();
	}

	private static void buildQuad(PoseStack.Pose pose, VertexConsumer consumer) {
		quadVertex(consumer, pose, 0, 0, 0, 0);
		quadVertex(consumer, pose, 0, 0, 1, 1);
		quadVertex(consumer, pose, 1, 0, 1, 1);
		quadVertex(consumer, pose, 1, 0, 0, 1);
	}

	private static void quadVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, int u, int v) {
		consumer.addVertex(pose, x, y, 0)
				.setColor(0, 255, 0, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(0xF000F0)
				.setNormal(pose, 0F, 0F, 1F);
	}

	/**
	 * Resolves which decal this player should get, or {@code null} when the layer does not apply.
	 * <p>
	 * 26.3 removed {@code getTextureLocation(AbstractClientPlayer)}; the entity is looked up from the render state's id so
	 * the UUID check still works.
	 */
	private static Identifier getTextureLocation(AvatarRenderState state) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			return YUE_LOC;
		}
		Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(state.id);
		if (!(entity instanceof Player player)) {
			return null;
		}
		if (SIN_UUID.equals(player.getUUID()) || CLAR_UUID.equals(player.getUUID())) {
			return CLAR_UUID.equals(player.getUUID()) ? HEART_LOC : YUE_LOC;
		}
		return null;
	}
}
