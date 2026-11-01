package moze_intel.projecte.gameObjs.registration;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Carries the registry key of the entry currently being built to the block and item properties created for it.
 * <p>
 * 26.3 requires {@code BlockBehaviour.Properties#setId} and {@code Item.Properties#setId} to be called before the
 * block or item itself is constructed; the id feeds the loot table key and the description id, and
 * {@code effectiveDrops()} throws {@code Block id not set} when it is missing. The deferred register is the only
 * place that knows the key, while the properties objects are built deep inside the factories it invokes, so the key
 * travels through a thread local instead of being repeated at every registration site.
 */
public final class PEPropertyIds {

	private static final ThreadLocal<Deque<ResourceKey<?>>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

	private PEPropertyIds() {
	}

	public static void push(ResourceKey<?> key) {
		CURRENT.get().push(key);
	}

	public static void pop() {
		Deque<ResourceKey<?>> stack = CURRENT.get();
		stack.pop();
		if (stack.isEmpty()) {
			//drop the thread local again so a server thread that only registered once does not keep the deque alive
			CURRENT.remove();
		}
	}

	public static BlockBehaviour.Properties block(BlockBehaviour.Properties properties) {
		Deque<ResourceKey<?>> stack = CURRENT.get();
		if (stack.isEmpty()) {
			return properties;
		}
		return stack.peek().cast(Registries.BLOCK).map(properties::setId).orElse(properties);
	}

	public static Item.Properties item(Item.Properties properties) {
		Deque<ResourceKey<?>> stack = CURRENT.get();
		if (stack.isEmpty()) {
			return properties;
		}
		return stack.peek().cast(Registries.ITEM).map(properties::setId).orElse(properties);
	}
}
