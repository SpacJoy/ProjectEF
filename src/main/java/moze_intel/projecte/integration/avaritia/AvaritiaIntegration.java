package moze_intel.projecte.integration.avaritia;

import moze_intel.projecte.PECore;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;

/**
 * Supplies ProjectE's matter singularities through Re:Avaritia's data-driven singularity loader.
 * Re:Avaritia generates the compressor recipes from these definitions during data reload.
 */
public final class AvaritiaIntegration {

	public static final String MODID = "avaritia";

	private AvaritiaIntegration() {
	}

	public static void registerRecipePack() {
		if (!ResourceManagerHelper.registerBuiltinResourcePack(PECore.rl("avaritia"), PECore.MOD_CONTAINER,
				ResourcePackActivationType.ALWAYS_ENABLED)) {
			throw new IllegalStateException("Missing ProjectEF Avaritia compatibility data pack");
		}
	}
}
