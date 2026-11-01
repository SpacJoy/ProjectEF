package moze_intel.projecte.api.capabilities;

import java.util.function.BiFunction;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper around a context-less {@link EntityApiLookup} representing an entity capability exposed by ProjectEF.
 *
 * @param <A> Type of the api this capability provides access to.
 */
public final class PEEntityCapability<A> {

	private final EntityApiLookup<A, Void> lookup;

	private PEEntityCapability(EntityApiLookup<A, Void> lookup) {
		this.lookup = lookup;
	}

	/**
	 * Creates a new context-less entity capability.
	 */
	public static <A> PEEntityCapability<A> create(Identifier id, Class<A> apiClass) {
		return new PEEntityCapability<>(EntityApiLookup.get(id, apiClass, Void.class));
	}

	/**
	 * Queries the capability on the given entity.
	 *
	 * @return The api instance, or {@code null} if the entity does not expose this capability.
	 */
	@Nullable
	public A find(Entity entity) {
		return lookup.find(entity, null);
	}

	/**
	 * Registers a provider for the given entity type.
	 */
	public <T extends Entity> void registerForType(BiFunction<T, Void, A> provider, EntityType<T> type) {
		lookup.registerForType(provider, type);
	}

	/**
	 * @return The underlying {@link EntityApiLookup} for direct interoperability with other Fabric mods.
	 */
	public EntityApiLookup<A, Void> lookup() {
		return lookup;
	}
}
