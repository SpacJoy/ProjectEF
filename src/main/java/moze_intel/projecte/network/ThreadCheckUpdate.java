package moze_intel.projecte.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import moze_intel.projecte.PECore;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.util.TimeUtil;

public class ThreadCheckUpdate extends Thread {

	// Fallback update JSON URL; mirrors the updateJsonURL declared in fabric.mod.json.
	private static final String DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/wchiway/ProjectEF/mc1.21.1/update.json";
	private static final String curseURL = "https://www.curseforge.com/minecraft/mc-mods/projecte/files";
	private static volatile String targetVersion = null;
	private static volatile boolean hasSentMessage = false;

	public ThreadCheckUpdate() {
		this.setName("ProjectEF Update Checker Notifier");
	}

	@Override
	public void run() {
		//Update notifications are client-only; do not burn a network request on a dedicated server.
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
			return;
		}
		ModMetadata metadata = PECore.MOD_CONTAINER.getMetadata();
		String currentVersion = metadata.getVersion().getFriendlyString();
		String updateUrl = metadata.getContact().get("updateJsonURL").orElse(null);
		if (updateUrl == null || updateUrl.isBlank()) {
			updateUrl = DEFAULT_UPDATE_URL;
		}

		String latest = null;
		int tries = 0;
		do {
			String result = fetchLatestVersion(updateUrl, currentVersion);
			if (result != null) {
				latest = result;
				break;
			}
			try {
				Thread.sleep(TimeUtil.MILLISECONDS_PER_SECOND);
			} catch (InterruptedException ignored) {
			}
			tries++;
		} while (tries < 10);

		if (latest == null) {
			PECore.LOGGER.warn("Update check failed.");
			return;
		}

		targetVersion = latest;
	}

	/**
	 * Fetches the update JSON from the given URL and compares the "latest" version
	 * with the current mod version. Returns the target version string if outdated, null otherwise.
	 */
	private static String fetchLatestVersion(String updateUrl, String currentVersion) {
		try {
			URL url = new URI(updateUrl).toURL();
			try (InputStreamReader reader = new InputStreamReader(url.openStream())) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				String latest = json.get("latest").getAsString();
				if (isNewer(latest, currentVersion)) {
					return latest;
				}
			}
		} catch (IOException | URISyntaxException e) {
			PECore.LOGGER.debug("Failed to check for updates: {}", e.getMessage());
		}
		return null;
	}

	private static boolean isNewer(String latest, String current) {
		try {
			return SemanticVersion.parse(latest).compareTo(SemanticVersion.parse(current)) > 0;
		} catch (VersionParsingException e) {
			PECore.LOGGER.warn("Failed to compare versions '{}' and '{}': {}", current, latest, e.getMessage());
			return false;
		}
	}

	/**
	 * Registers the client-side entity load listener to show update notifications.
	 * Called by the client mod initializer (stage 4).
	 */
	public static void registerClient() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
			return;
		}
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof LocalPlayer player && targetVersion != null && !hasSentMessage) {
				hasSentMessage = true;
				player.sendSystemMessage(PELang.UPDATE_AVAILABLE.translate(targetVersion));
				player.sendSystemMessage(PELang.UPDATE_GET_IT.translate());
				player.sendSystemMessage(TextComponentUtil.build(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(curseURL)), curseURL));
			}
		});
	}
}
