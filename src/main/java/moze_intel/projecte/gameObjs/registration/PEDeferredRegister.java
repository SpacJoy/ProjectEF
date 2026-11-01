package moze_intel.projecte.gameObjs.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Collects registry entries up front and performs the actual registration against the vanilla registry when {@link #register()} is called from the mod
 * initializer. Replaces the NeoForge DeferredRegister while keeping the same call sites.
 */
public class PEDeferredRegister<T> {

	public static <T> PEDeferredRegister<T> create(@NotNull ResourceKey<? extends Registry<T>> registryKey, @NotNull String namespace) {
		return new PEDeferredRegister<>(registryKey, namespace);
	}

	@NotNull
	private final Function<ResourceKey<T>, ? extends PEDeferredHolder<T, ?>> holderCreator;
	@NotNull
	private final ResourceKey<? extends Registry<T>> registryKey;
	@NotNull
	private final String namespace;
	private final List<Entry<T, ?>> entries = new ArrayList<>();
	private final List<PEDeferredHolder<T, ? extends T>> holders = new ArrayList<>();
	private boolean registered;

	public PEDeferredRegister(@NotNull ResourceKey<? extends Registry<T>> registryKey, @NotNull String namespace) {
		this(registryKey, namespace, PEDeferredHolder::new);
	}

	public PEDeferredRegister(@NotNull ResourceKey<? extends Registry<T>> registryKey, @NotNull String namespace,
			@NotNull Function<ResourceKey<T>, ? extends PEDeferredHolder<T, ? extends T>> holderCreator) {
		this.registryKey = registryKey;
		this.namespace = namespace;
		this.holderCreator = holderCreator;
	}

	@NotNull
	public <I extends T> PEDeferredHolder<T, I> register(@NotNull String name, @NotNull Supplier<? extends I> sup) {
		return register(name, key -> sup.get());
	}

	@NotNull
	@SuppressWarnings("unchecked")
	public <I extends T> PEDeferredHolder<T, I> register(@NotNull String name, @NotNull Function<Identifier, ? extends I> func) {
		if (registered) {
			throw new IllegalStateException("Cannot add entries to " + registryKey + " after registration has been performed");
		}
		Identifier valueName = Identifier.fromNamespaceAndPath(namespace, name);
		PEDeferredHolder<T, I> holder = (PEDeferredHolder<T, I>) createHolder(registryKey, valueName);
		entries.add(new Entry<>(holder, func));
		holders.add(holder);
		return holder;
	}

	@NotNull
	@SuppressWarnings("unchecked")
	protected <I extends T> PEDeferredHolder<T, I> createHolder(@NotNull ResourceKey<? extends Registry<T>> registryKey, @NotNull Identifier key) {
		return (PEDeferredHolder<T, I>) holderCreator.apply(ResourceKey.create(registryKey, key));
	}

	/**
	 * Performs the actual registration of all collected entries against the vanilla registry. Call once from the mod initializer, in dependency order relative to
	 * other registers (for example blocks before items before block entity types).
	 */
	public void register() {
		if (registered) {
			throw new IllegalStateException("Duplicate registration for " + registryKey);
		}
		registered = true;
		Registry<T> registry = findRegistry();
		for (Entry<T, ?> entry : entries) {
			entry.register(registry);
		}
	}

	@SuppressWarnings("unchecked")
	private Registry<T> findRegistry() {
		Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
		if (registry == null) {
			throw new IllegalStateException("Cannot find registry " + registryKey);
		}
		return registry;
	}

	@NotNull
	public Collection<PEDeferredHolder<T, ? extends T>> getEntries() {
		return Collections.unmodifiableList(holders);
	}

	@NotNull
	public ResourceKey<? extends Registry<T>> getRegistryKey() {
		return registryKey;
	}

	@NotNull
	public String getNamespace() {
		return namespace;
	}

	private record Entry<T, I extends T>(PEDeferredHolder<T, I> holder, Function<Identifier, ? extends I> factory) {

		private void register(Registry<T> registry) {
			//26.3 needs the registry key on the block and item properties, so it is published for the duration of the
			//factory call and picked up again by the block and item constructors
			PEPropertyIds.push(holder.getKey());
			I value;
			try {
				value = factory.apply(holder.getId());
			} finally {
				PEPropertyIds.pop();
			}
			Holder.Reference<T> reference = Registry.registerForHolder(registry, holder.getKey(), value);
			holder.bind(value, reference);
		}
	}
}
