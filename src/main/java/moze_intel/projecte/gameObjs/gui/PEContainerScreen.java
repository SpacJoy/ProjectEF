package moze_intel.projecte.gameObjs.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public abstract class PEContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

	public boolean switchingToJEI;

	public PEContainerScreen(T container, Inventory invPlayer, Component title) {
		super(container, invPlayer, title);
	}

	/**
	 * 26.3 made {@code imageWidth}/{@code imageHeight} final on {@link AbstractContainerScreen}: subclasses can no
	 * longer stretch the panel after construction, so the size has to travel through the super constructor instead.
	 */
	public PEContainerScreen(T container, Inventory invPlayer, Component title, int imageWidth, int imageHeight) {
		super(container, invPlayer, title, imageWidth, imageHeight);
	}

	@Override
	public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		this.extractTooltip(graphics, mouseX, mouseY);
	}

	/**
	 * 26.3 dropped the six-int {@code blit(Identifier, x, y, u, v, w, h)} overload: every blit now needs an explicit
	 * {@link net.minecraft.client.renderer.RenderPipelines pipeline} plus the texture sheet size. All of our GUI
	 * textures are plain 256x256 sheets, so the legacy call maps 1:1 onto GUI_TEXTURED.
	 */
	protected static void blitGui(@NotNull GuiGraphicsExtractor graphics, @NotNull Identifier texture, int x, int y, int u, int v, int width, int height) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
	}

	@Override
	public void removed() {
		if (!switchingToJEI) {
			//If we are not switching to JEI then run the super close method
			// which will exit the container. We don't want to mark the
			// container as exited if it will be revived when leaving JEI
			super.removed();
		}
	}

	//Note: Technically this really should be init(@NotNull Minecraft minecraft, int width, int height)
	// but given we don't actually have any data that would use the resize params we can get away with
	// just resetting the switchingToJEI here
	@Override
	public void init() {
		//Mark that we are not switching to JEI if we start being initialized again
		switchingToJEI = false;
		super.init();
	}
}
