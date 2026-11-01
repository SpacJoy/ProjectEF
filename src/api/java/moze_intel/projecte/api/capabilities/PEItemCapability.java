package moze_intel.projecte.api.capabilities;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper around a context-less {@link ItemApiLookup} representing an item capability exposed by ProjectEF.
 *
 * @param <A> Type of the api this capability provides access to.
 */
public final class PEItemCapability<A> {

	private final ItemApiLookup<A, Void> lookup;

	private PEItemCapability(ItemApiLookup<A, Void> lookup) {
		this.lookup = lookup;
	}

	/**
	 * Creates a new context-less item capability.
	 */
	public static <A> PEItemCapability<A> create(Identifier id, Class<A> apiClass) {
		return new PEItemCapability<>(ItemApiLookup.get(id, apiClass, Void.class));
	}

	/**
	 * Queries the capability on the given stack.
	 *
	 * @return The api instance, or {@code null} if the stack does not expose this capability.
	 */
	@Nullable
	public A find(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		return lookup.find(stack, null);
	}

	/**
	 * Registers a provider for the given items.
	 */
	public void registerForItems(ItemApiLookup.ItemApiProvider<A, Void> provider, ItemLike... items) {
		lookup.registerForItems(provider, items);
	}

	/**
	 * Registers a fallback provider, queried when no item specific provider matched.
	 */
	public void registerFallback(ItemApiLookup.ItemApiProvider<A, Void> provider) {
		lookup.registerFallback(provider);
	}

	/**
	 * @return The underlying {@link ItemApiLookup} for direct interoperability with other Fabric mods.
	 */
	public ItemApiLookup<A, Void> lookup() {
		return lookup;
	}
}
