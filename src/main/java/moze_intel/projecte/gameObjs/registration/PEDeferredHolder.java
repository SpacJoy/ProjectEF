package moze_intel.projecte.gameObjs.registration;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A lazily bound holder for a registry object. Mirrors the behavior the NeoForge DeferredHolder used to provide: created up front, bound when the owning
 * {@link PEDeferredRegister} performs the actual registration against the vanilla registry.
 *
 * <p>26.3 turned {@link Holder} into a sealed interface that only permits {@code Holder$Direct} and {@code Holder$Reference}, so this class can no longer
 * implement it. Use {@link #asReference()} wherever a real vanilla holder is required.
 */
public class PEDeferredHolder<R, T extends R> implements INamedEntry, Supplier<T> {

	private final ResourceKey<R> key;
	@Nullable
	private T value;
	@Nullable
	private Holder.Reference<R> reference;

	public PEDeferredHolder(ResourceKey<? extends Registry<R>> registryKey, Identifier valueName) {
		this(ResourceKey.create(registryKey, valueName));
	}

	public PEDeferredHolder(ResourceKey<R> key) {
		this.key = key;
	}

	/**
	 * Binds this holder to the registered value. Called by {@link PEDeferredRegister} when registration is performed.
	 */
	void bind(T value, Holder.Reference<R> reference) {
		this.value = value;
		this.reference = reference;
	}

	/**
	 * @return The vanilla holder this object was bound to.
	 */
	@NotNull
	public Holder.Reference<R> asReference() {
		if (reference == null) {
			throw new IllegalStateException("Registry object not present: " + key + ". Was the owning register's register() method called yet?");
		}
		return reference;
	}

	/**
	 * @return The registered value.
	 */
	@Override
	public T get() {
		return value();
	}

	@NotNull
	public T value() {
		if (value == null) {
			throw new IllegalStateException("Registry object not present: " + key + ". Was the owning register's register() method called yet?");
		}
		return value;
	}

	public Identifier getId() {
		return key.identifier();
	}

	public ResourceKey<R> getKey() {
		return key;
	}

	public Optional<T> asOptional() {
		return Optional.ofNullable(value);
	}

	@Override
	public String getName() {
		return getId().getPath();
	}

	public boolean isBound() {
		return value != null;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof PEDeferredHolder<?, ?> other && other.key == this.key;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		return "PEDeferredHolder{" + key + "}";
	}
}
