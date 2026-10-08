package moze_intel.projecte.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMap;
import it.unimi.dsi.fastutil.objects.Object2LongSortedMaps.UnmodifiableSortedMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import moze_intel.projecte.api.codec.IPECodecHelper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.impl.codec.PECodecHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

public final class CustomEMCParser {

	private static final Path CONFIG = ProjectEConfig.CONFIG_DIR.resolve("custom_emc.json");

	public record CustomEMCFile(Object2LongSortedMap<NSSItem> entries, @Nullable String comment) {

		public static final Codec<CustomEMCFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ExtraCodecs.NON_EMPTY_STRING.lenientOptionalFieldOf("comment").forGetter(file -> Optional.ofNullable(file.comment)),
				//Skip invalid keys
				IPECodecHelper.INSTANCE.<NSSItem, Long, Object2LongSortedMap<NSSItem>>modifiableMap(IPECodecHelper.INSTANCE.lenientKeyUnboundedMap(
						NSSItem.CODEC,
						IPECodecHelper.INSTANCE.nonNegativeLong().fieldOf("emc")
				), Object2LongLinkedOpenHashMap::new).fieldOf("entries").forGetter(CustomEMCFile::entries)
		).apply(instance, (comment, entries) -> new CustomEMCFile(entries, comment.orElse(null))));

		public CustomEMCFile {
			if (!(entries instanceof UnmodifiableSortedMap)) {
				entries.defaultReturnValue(-1);
			}
		}
	}

	public static CustomEMCFile currentEntries;
	private static boolean dirty = false;
	private static boolean needsRemap = false;

	private static CustomEMCFile createDefault() {
		return new CustomEMCFile(new Object2LongLinkedOpenHashMap<>(), "Use the in-game commands to edit this file");
	}

	public static void init(HolderLookup.Provider registries) {
		if (!flushChanges(registries)) {
			return;
		}

		if (Files.exists(CONFIG)) {
			currentEntries = PECodecHelper.readFromFile(registries, CONFIG, CustomEMCFile.CODEC, "custom emc")
					.orElseGet(CustomEMCParser::createDefault);
		} else {
			currentEntries = createDefault();
			PECodecHelper.writeToFile(registries, CONFIG, CustomEMCFile.CODEC, currentEntries, "default custom EMC");
		}
	}

	public static void addToFile(NSSItem toAdd, long emc) {
		if (emc < 0) {
			throw new IllegalArgumentException("EMC must be non-negative: " + emc);
		}
		long old = currentEntries.entries().put(toAdd, emc);
		if (old == -1 || old != emc) {
			dirty = true;
			needsRemap = true;
		}
	}

	public static boolean removeFromFile(NSSItem toRemove) {
		boolean removed = currentEntries.entries().removeLong(toRemove) != -1;
		if (removed) {
			dirty = true;
			needsRemap = true;
		}
		return removed;
	}

	public static void flush(HolderLookup.Provider registries) {
		flushChanges(registries);
	}

	public static boolean flushChanges(HolderLookup.Provider registries) {
		if (dirty) {
			if (!PECodecHelper.tryWriteToFile(registries, CONFIG, CustomEMCFile.CODEC, currentEntries, "custom EMC")) {
				return false;
			}
			dirty = false;
		}
		return true;
	}

	/**
	 * Removes every override whose base item belongs to the given namespace (mod id). Tags and component variants of the same namespace are removed as well.
	 *
	 * @return The number of entries removed, {@code 0} when there was nothing to remove, or {@code -1} when the write failed (the previous state is restored).
	 */
	public static int removeNamespace(HolderLookup.Provider registries, String namespace) {
		if (currentEntries == null) {
			return 0;
		}
		List<NSSItem> toRemove = new ArrayList<>();
		for (NSSItem item : currentEntries.entries().keySet()) {
			if (!item.representsTag() && item.getResourceLocation().getNamespace().equals(namespace)) {
				toRemove.add(item);
			}
		}
		if (toRemove.isEmpty()) {
			return 0;
		}
		boolean wasDirty = dirty;
		boolean wasPending = needsRemap;
		Map<NSSItem, Long> previous = new LinkedHashMap<>(toRemove.size());
		for (NSSItem item : toRemove) {
			previous.put(item, currentEntries.entries().removeLong(item));
		}
		dirty = true;
		needsRemap = true;
		if (flushChanges(registries)) {
			return toRemove.size();
		}
		for (Map.Entry<NSSItem, Long> entry : previous.entrySet()) {
			currentEntries.entries().put(entry.getKey(), entry.getValue());
		}
		dirty = wasDirty;
		needsRemap = wasPending;
		return -1;
	}

	/** Saves one exact override; -1 removes it. A failed write leaves the previous state intact. */
	public static boolean saveChange(HolderLookup.Provider registries, NSSItem item, long value) {
		long previous = currentEntries.entries().getLong(item);
		boolean wasDirty = dirty;
		boolean wasPending = needsRemap;
		if (value == -1) {
			removeFromFile(item);
		} else {
			addToFile(item, value);
		}
		if (flushChanges(registries)) {
			return true;
		}
		if (previous == -1) {
			currentEntries.entries().removeLong(item);
		} else {
			currentEntries.entries().put(item, previous);
		}
		dirty = wasDirty;
		needsRemap = wasPending;
		return false;
	}

	public static boolean needsRemap() {
		return needsRemap;
	}

	public static void markRemapped() {
		needsRemap = false;
	}
}