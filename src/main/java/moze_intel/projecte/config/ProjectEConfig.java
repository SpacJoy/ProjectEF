package moze_intel.projecte.config;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import moze_intel.projecte.PECore;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

public class ProjectEConfig {

	public static final Path CONFIG_DIR = createConfigDir();
	private static final Map<IConfigSpec, IPEConfig> KNOWN_CONFIGS = new HashMap<>();

	public static final ServerConfig server = new ServerConfig();
	public static final CommonConfig common = new CommonConfig();
	public static final ClientConfig client = new ClientConfig();

	private static Path createConfigDir() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(PECore.CONFIG_DIR_NAME);
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to create " + PECore.CONFIG_DIR_NAME + " config directory", e);
		}
		return path;
	}

	public static void register() {
		registerConfig(server);
		registerConfig(common);
		registerConfig(client);
		//Note: We listen to both the initial load and the reload, to make sure that we fix any accidentally
		// cached values from calls before the initial loading
		ModConfigEvents.loading(PECore.MODID).register(config -> onConfigLoad(config, false));
		ModConfigEvents.reloading(PECore.MODID).register(config -> onConfigLoad(config, false));
		ModConfigEvents.unloading(PECore.MODID).register(config -> onConfigLoad(config, true));
	}

	public static Collection<IPEConfig> getConfigs() {
		return Collections.unmodifiableCollection(KNOWN_CONFIGS.values());
	}

	/**
	 * Creates and registers a mod config, and tracks it so that we can properly clear cached values.
	 */
	public static void registerConfig(IPEConfig config) {
		ConfigRegistry.INSTANCE.register(PECore.MODID, config.getConfigType(), config.getConfigSpec(), PECore.CONFIG_DIR_NAME + "/" + config.getFileName() + ".toml");
		KNOWN_CONFIGS.put(config.getConfigSpec(), config);
	}

	private static void onConfigLoad(ModConfig config, boolean unloading) {
		//Make sure it is for the same modid as us
		if (config.getModId().equals(PECore.MODID)) {
			IPEConfig peConfig = KNOWN_CONFIGS.get(config.getSpec());
			if (peConfig != null) {
				peConfig.clearCache(unloading);
			}
		}
	}
}
