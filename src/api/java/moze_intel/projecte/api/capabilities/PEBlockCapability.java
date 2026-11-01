package moze_intel.projecte.api.capabilities;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper around a {@link BlockApiLookup} representing a block capability exposed by ProjectEF.
 *
 * @param <A> Type of the api this capability provides access to.
 * @param <C> Type of the additional context (for example the side being queried).
 */
public final class PEBlockCapability<A, C> {

	private final BlockApiLookup<A, C> lookup;

	private PEBlockCapability(BlockApiLookup<A, C> lookup) {
		this.lookup = lookup;
	}

	/**
	 * Creates a new block capability with the given context class.
	 */
	public static <A, C> PEBlockCapability<A, C> create(Identifier id, Class<A> apiClass, Class<C> contextClass) {
		return new PEBlockCapability<>(BlockApiLookup.get(id, apiClass, contextClass));
	}

	/**
	 * Queries the capability at the given position.
	 *
	 * @return The api instance, or {@code null} if the block does not expose this capability.
	 */
	@Nullable
	public A find(Level level, BlockPos pos, C context) {
		return lookup.find(level, pos, context);
	}

	/**
	 * Queries the capability at the given position with known state/block entity to avoid extra lookups.
	 *
	 * @return The api instance, or {@code null} if the block does not expose this capability.
	 */
	@Nullable
	public A find(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, C context) {
		return lookup.find(level, pos, state, blockEntity, context);
	}

	/**
	 * Registers a provider for the given block entity types.
	 */
	public void registerForBlockEntities(BlockApiLookup.BlockEntityApiProvider<A, C> provider, BlockEntityType<?>... types) {
		lookup.registerForBlockEntities(provider, types);
	}

	/**
	 * @return The underlying {@link BlockApiLookup} for direct interoperability with other Fabric mods.
	 */
	public BlockApiLookup<A, C> lookup() {
		return lookup;
	}
}
