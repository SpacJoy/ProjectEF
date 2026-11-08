package moze_intel.projecte.rendering;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Holds the render types ProjectE needs that vanilla does not expose directly.
 * <p>
 * 26.3 replaced the old {@code RenderType.CompositeState}/{@code RenderStateShard} builder with {@link
 * net.minecraft.client.renderer.rendertype.RenderSetup}, and a {@code RenderType} is now just a thin wrapper over a
 * {@link com.mojang.renderpearl.api.pipeline.RenderPipeline}. Custom render types can no longer be assembled from
 * shard constants at runtime, so everything here is expressed through the vanilla {@link RenderTypes} factories:
 * <ul>
 *   <li>{@link #sprite} — a plain cutout texture quad, used by {@link EntitySpriteRenderer}.</li>
 *   <li>{@link #glowingSprite} — a translucent, non-culled textured quad, used by {@link LayerYue}. Only the alpha
 *   differs from {@link #sprite}, which is what the old hand-rolled POSITION_TEX_COLOR shard pair was for.</li>
 * </ul>
 * Both used to be built with a {@code Function<Identifier, RenderType>} memoized cache; the vanilla factories already
 * cache internally, so the functions are gone and callers resolve the type directly from their texture.
 */
public class PERenderType {

	private PERenderType() {
	}

	/**
	 * Cutout texture quad that ignores lighting entirely.
	 */
	public static RenderType sprite(Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}

	/**
	 * Translucent textured quad that is not culled; used for the flat circle decals rendered on top of a player body.
	 */
	public static RenderType glowingSprite(Identifier texture) {
		return RenderTypes.entityTranslucent(texture);
	}

	/**
	 * Translucent no-cull overlay used to highlight the blocks the philosopher's stone would transmute.
	 * <p>
	 * 26.3 lost {@code LevelRenderer#renderFace}, so the geometry is now emitted by hand from {@link
	 * TransmutationRenderingOverlay}. The overlay is also untextured now: 26.3 (like 1.21.10 before it) removed
	 * {@code textures/misc/white.png} from the vanilla assets, so the old "translucent entity quad over a white texture"
	 * trick left the overlay binding a missing texture, which is why it rendered as the black/magenta checkerboard.
	 * Vanilla's debug filled box is exactly the shape needed: a POSITION_COLOR quad pipeline that is unlit, blended, not
	 * culled, does not write depth and carries a view offset so the overlay does not z-fight with the block it sits on.
	 * This mirrors the hand-built POSITION_COLOR render type the 1.21.1 branch had.
	 */
	public static final RenderType TRANSMUTATION_OVERLAY = RenderTypes.debugFilledBox();
}
