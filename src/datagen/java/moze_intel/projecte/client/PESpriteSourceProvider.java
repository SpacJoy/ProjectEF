package moze_intel.projecte.client;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import moze_intel.projecte.PECore;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;

public class PESpriteSourceProvider extends SpriteSourceProvider {

	private final Set<Identifier> trackedSingles = new HashSet<>();

	public PESpriteSourceProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper fileHelper) {
		super(output, lookupProvider, PECore.MODID, fileHelper);
	}

	@Override
	protected void gather() {
		//26.3 note: slot icons go through GuiGraphicsExtractor#blitSprite, which only resolves sprites from the gui atlas.
		// The curios icons therefore sit under textures/gui/sprites/curios and are picked up by the vanilla gui.json
		// directory source, so no extra atlas entry is generated here.
	}

	protected void addFiles(SourceList atlas, Identifier... resourceLocations) {
		for (Identifier rl : resourceLocations) {
			//Only add this source if we haven't already added it as a direct single file source
			if (trackedSingles.add(rl)) {
				atlas.addSource(new SingleFile(rl, Optional.empty()));
			}
		}
	}

	protected void addDirectory(SourceList atlas, String directory, String spritePrefix) {
		atlas.addSource(new DirectoryLister(directory, spritePrefix));
	}
}