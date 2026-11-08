package moze_intel.projecte.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.PhilosophersStone;
import moze_intel.projecte.gameObjs.items.PhilosophersStone.PhilosophersStoneMode;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.Constants;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TransmutationRenderingOverlay {

	private final Minecraft mc = Minecraft.getInstance();
	@Nullable
	private Block transmutationResult;
	private long lastGameTime;

	/**
	 * Renders the little preview of what the philosopher's stone will transmute the targeted block into, hooked into the hud render callback.
	 */
	public void onHudRender(@NotNull GuiGraphicsExtractor graphics, @NotNull DeltaTracker delta) {
		//26.3 moved the "hide GUI" flag off Options and onto the HUD itself.
		if (!mc.gui.hud.isHidden() && transmutationResult != null) {
			if (transmutationResult instanceof LiquidBlock liquidBlock) {
				FluidState fluidState = liquidBlock.fluid.defaultFluidState();
				//26.3 dropped Fabric's FluidRenderHandler#getFluidColor/getFluidSprites: fluid appearance is now a baked
				// FluidModel reached through the vanilla model manager, carrying the sprite and the tint source.
				FluidModel fluidModel = mc.getModelManager().getFluidStateModelSet().get(fluidState);
				TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
				//26.3 only bakes a tint source for fluids that actually have one: the vanilla lava model is built with a
				// null tint source, so calling through it unconditionally crashed the frame as soon as the preview
				// targeted something that transmutes into lava (the obsidian of a nether portal frame, for example).
				// Vanilla's FluidRenderer does the same null check and falls back to -1, so lava previews untinted.
				BlockTintSource tintSource = fluidModel.tintSource();
				int rgb = tintSource == null ? 0xFFFFFF : tintSource.color(transmutationResult.defaultBlockState()) & 0xFFFFFF;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 1, 1, 16, 16, rgb | 0xFF000000);
			} else {
				//Just render it normally instead of with the given model as some block's don't render properly then as an item
				// for example glass panes
				graphics.item(new ItemStack(transmutationResult), 1, 1);
			}
			long gameTime = mc.level == null ? 0 : mc.level.getGameTime();
			if (lastGameTime != gameTime) {
				//If the game time changed, so we aren't actually still hovering a block set our
				// result to null. We do this after rendering it just in case there is a single
				// frame where this may actually be valid based on the order the events are fired
				transmutationResult = null;
				lastGameTime = gameTime;
			}
		}
	}

	/**
	 * Highlights the blocks the philosopher's stone would change and remembers the result for the hud preview.
	 * <p>
	 * 26.3 removed {@code MultiBufferSource}, {@code WorldRenderContext} and {@code LevelRenderer#renderFace}. The hook now
	 * receives a {@link LevelRenderContext} carrying the {@link net.minecraft.client.renderer.SubmitNodeCollector} and the
	 * world {@link PoseStack}, and the overlay quads are emitted by hand through
	 * {@link net.minecraft.client.renderer.SubmitNodeCollector#submitCustomGeometry}.
	 *
	 * @return always true so the vanilla outline still renders.
	 */
	public boolean onBlockOutline(LevelRenderContext context, BlockOutlineRenderState outlineState) {
		Player player = mc.player;
		if (player == null) {
			return true;
		}
		Level level = player.level();
		lastGameTime = level.getGameTime();
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			stack = player.getOffhandItem();
		}
		if (stack.isEmpty() || !stack.is(PEItems.PHILOSOPHERS_STONE.get())) {
			transmutationResult = null;
			return true;
		}
		boolean isSneaking = player.isSecondaryUseActive();
		PhilosophersStone philoStone = (PhilosophersStone) stack.getItem();
		//Note: We use the philo stone's ray trace instead of the outline's position as we want to make sure that we
		// can properly take fluid into account/ignore it when needed
		BlockHitResult rtr = philoStone.getHitBlock(level, player, isSneaking);
		if (rtr.getType() == HitResult.Type.BLOCK) {
			int charge = philoStone.getCharge(stack);
			PhilosophersStoneMode mode = philoStone.getMode(stack);
			Object2ReferenceMap<BlockPos, BlockState> changes = PhilosophersStone.getChanges(level, rtr.getBlockPos(), rtr.getDirection(), player.getDirection(),
					isSneaking, mode, charge);
			if (changes.isEmpty()) {
				transmutationResult = null;
			} else {
				transmutationResult = changes.values().iterator().next().getBlock();
				//26.3 renamed Camera#getPosition to position
				Vec3 viewPosition = mc.gameRenderer.mainCamera().position();
				float alpha = ProjectEConfig.client.pulsatingOverlay.get() ? getPulseProportion() * 0.60F : 0.35F;
				CollisionContext selectionContext = CollisionContext.of(player);
				PoseStack pose = context.poseStack();
				for (BlockPos pos : changes.keySet()) {
					BlockState state = level.getBlockState(pos);
					if (!state.isAir()) {
						VoxelShape shape = state.getShape(level, pos, selectionContext);
						if (!shape.isEmpty()) {
							pose.pushPose();
							//Shift by view position here so that we don't have floating point issues at large values
							pose.translate(pos.getX() - viewPosition.x, pos.getY() - viewPosition.y, pos.getZ() - viewPosition.z);
							context.submitNodeCollector().submitCustomGeometry(pose, PERenderType.TRANSMUTATION_OVERLAY,
									(poseEntry, consumer) -> emitShape(poseEntry, consumer, shape, alpha));
							pose.popPose();
						}
					}
				}
			}
		} else {
			transmutationResult = null;
		}
		return true;
	}

	/**
	 * Emits every face of the given shape as two triangles worth of quads.
	 * <p>
	 * 26.3 dropped {@code LevelRenderer#renderFace}, so the face winding is reproduced here. The overlay render type is
	 * POSITION_COLOR only, so unlike the old 1.21.1 code there is no full-bright lightmap or normal to supply: {@code
	 * BufferBuilder} silently skips vertex elements the pipeline format does not declare. Colour is white with the
	 * pulsating alpha and the pipeline blends it over the world.
	 */
	private static void emitShape(PoseStack.Pose pose, VertexConsumer consumer, VoxelShape shape, float alpha) {
		int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			for (Direction direction : Constants.DIRECTIONS) {
				emitFace(pose, consumer, direction, color, (float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ);
			}
		});
	}

	/**
	 * Emits a single box face, wound the same way as vanilla so back-face culling does not drop it.
	 */
	private static void emitFace(PoseStack.Pose pose, VertexConsumer consumer, Direction direction, int color,
			float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
		float x0 = minX, y0 = minY, z0 = minZ, x1 = maxX, y1 = maxY, z1 = maxZ;
		switch (direction) {
			case DOWN -> {
				vertex(pose, consumer, color, x0, y0, z0);
				vertex(pose, consumer, color, x0, y0, z1);
				vertex(pose, consumer, color, x1, y0, z1);
				vertex(pose, consumer, color, x1, y0, z0);
			}
			case UP -> {
				vertex(pose, consumer, color, x0, y1, z0);
				vertex(pose, consumer, color, x1, y1, z0);
				vertex(pose, consumer, color, x1, y1, z1);
				vertex(pose, consumer, color, x0, y1, z1);
			}
			case NORTH -> {
				vertex(pose, consumer, color, x0, y0, z0);
				vertex(pose, consumer, color, x1, y0, z0);
				vertex(pose, consumer, color, x1, y1, z0);
				vertex(pose, consumer, color, x0, y1, z0);
			}
			case SOUTH -> {
				vertex(pose, consumer, color, x0, y0, z1);
				vertex(pose, consumer, color, x0, y1, z1);
				vertex(pose, consumer, color, x1, y1, z1);
				vertex(pose, consumer, color, x1, y0, z1);
			}
			case WEST -> {
				vertex(pose, consumer, color, x0, y0, z0);
				vertex(pose, consumer, color, x0, y1, z0);
				vertex(pose, consumer, color, x0, y1, z1);
				vertex(pose, consumer, color, x0, y0, z1);
			}
			case EAST -> {
				vertex(pose, consumer, color, x1, y0, z0);
				vertex(pose, consumer, color, x1, y0, z1);
				vertex(pose, consumer, color, x1, y1, z1);
				vertex(pose, consumer, color, x1, y1, z0);
			}
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int color, float x, float y, float z) {
		consumer.addVertex(pose, x, y, z).setColor(color);
	}

	private float getPulseProportion() {
		return (float) (0.5F * Math.sin(System.currentTimeMillis() / 350.0) + 0.5F);
	}
}
