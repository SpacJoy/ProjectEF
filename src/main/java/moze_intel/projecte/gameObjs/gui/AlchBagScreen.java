package moze_intel.projecte.gameObjs.gui;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AlchBagScreen extends PEContainerScreen<AlchBagContainer> {

	private static final Identifier texture = PECore.rl("textures/gui/alchchest.png");

	public AlchBagScreen(AlchBagContainer container, Inventory invPlayer, Component title) {
		super(container, invPlayer, title, 255, 230);
	}

	@Override
	public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		blitGui(graphics, texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int x, int y) {
		//Don't render title or inventory as we don't have space
	}
}