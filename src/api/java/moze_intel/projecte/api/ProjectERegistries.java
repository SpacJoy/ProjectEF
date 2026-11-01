package moze_intel.projecte.api;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public class ProjectERegistries {

	private ProjectERegistries() {
	}

	private static Identifier rl(String path) {
		return Identifier.fromNamespaceAndPath(ProjectEAPI.PROJECTE_MODID, path);
	}

	/**
	 * Gets the {@link ResourceKey} representing the name of the Registry for serializing {@link NormalizedSimpleStack}s.
	 */
	public static final ResourceKey<Registry<MapCodec<? extends NormalizedSimpleStack>>> NSS_SERIALIZER_NAME = ResourceKey.createRegistryKey(rl("nss_serializer"));

	/**
	 * Gets the Registry for serializing {@link NormalizedSimpleStack}s.
	 *
	 * @see #NSS_SERIALIZER_NAME
	 */
	public static final Registry<MapCodec<? extends NormalizedSimpleStack>> NSS_SERIALIZER = FabricRegistryBuilder.createDefaulted(NSS_SERIALIZER_NAME,
			rl("item"))//Default to item serialization
			.buildAndRegister();
}
