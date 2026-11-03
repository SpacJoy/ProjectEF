package moze_intel.projecte.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import moze_intel.projecte.PECore;
import moze_intel.projecte.config.value.CachedValue;
import net.neoforged.neoforge.common.ModConfigSpec;

public abstract class BasePEConfig implements IPEConfig {

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		// Daemon thread: a pending config write must never keep the JVM alive after shutdown.
		// On MC 26.2+ vanilla no longer calls System.exit(0), so a non-daemon pool left running by this
		// class makes ClientShutdownWatchdog time out ("Watchdog (Client shutdown from post-main)").
		Thread thread = new Thread(runnable, "ProjectE-Config-Saver");
		thread.setDaemon(true);
		return thread;
	});

	private final List<CachedValue<?>> cachedConfigValues = new ArrayList<>();

	@Override
	public void clearCache(boolean unloading) {
		for (CachedValue<?> cachedConfigValue : cachedConfigValues) {
			cachedConfigValue.clearCache(unloading);
		}
	}

	@Override
	public <T> void addCachedValue(CachedValue<T> configValue) {
		cachedConfigValues.add(configValue);
	}

	@Override
	public void save() {
		EXECUTOR.submit(new ConfigSaver(getConfigSpec()));
	}

	private static class ConfigSaver implements Runnable {

		private final ModConfigSpec configSpec;
		private int retries = 0;

		private ConfigSaver(ModConfigSpec configSpec) {
			this.configSpec = configSpec;
		}

		@Override
		public void run() {
			try {
				configSpec.save();
			} catch (Exception e) {
				PECore.LOGGER.error("Failed to save config", e);
				if (retries++ < 3) {
					EXECUTOR.submit(this);
				} else {
					PECore.LOGGER.error("Giving up");
				}
			}
		}
	}
}