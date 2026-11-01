package moze_intel.projecte.gameObjs.registration.impl;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import moze_intel.projecte.api.capabilities.PEBlockCapability;
import moze_intel.projecte.gameObjs.registration.PEDeferredHolder;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.BlockEntityTypeRegistryObject.CapabilityData;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import org.jetbrains.annotations.Nullable;

public class BlockEntityTypeDeferredRegister extends PEDeferredRegister<BlockEntityType<?>> {

	public BlockEntityTypeDeferredRegister(String modid) {
		super(Registries.BLOCK_ENTITY_TYPE, modid, BlockEntityTypeRegistryObject::new);
	}

	public <BE extends BlockEntity> BlockEntityTypeBuilder<BE> builder(BlockRegistryObject<?, ?> block, BlockEntitySupplier<? extends BE> factory) {
		return new BlockEntityTypeBuilder<>(block, factory);
	}

	@SuppressWarnings("unchecked")
	private <BE extends BlockEntity> BlockEntityTypeRegistryObject<BE> registerPE(String name, Supplier<? extends BlockEntityType<BE>> sup) {
		return (BlockEntityTypeRegistryObject<BE>) super.register(name, sup);
	}

	@Override
	public void register() {
		super.register();
		registerCapabilities();
	}

	private void registerCapabilities() {
		for (PEDeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> entry : getEntries()) {
			//Note: All entries should be of this type
			if (entry instanceof BlockEntityTypeRegistryObject<?> beRO) {
				beRO.registerCapabilityProviders();
			} else if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				throw new IllegalStateException("Expected entry to be a BlockEntityTypeRegistryObject");
			}
		}
	}

	public class BlockEntityTypeBuilder<BE extends BlockEntity> {

		private static final BiFunction<?, ?, ?> SIMPLE_PROVIDER = (obj, context) -> obj;

		private final BlockRegistryObject<?, ?> block;
		private final BlockEntitySupplier<? extends BE> factory;
		private final List<CapabilityData<BE, ?, ?>> capabilityProviders = new ArrayList<>();
		@Nullable
		private BlockEntityTicker<BE> clientTicker;
		@Nullable
		private BlockEntityTicker<BE> serverTicker;

		BlockEntityTypeBuilder(BlockRegistryObject<?, ?> block, BlockEntitySupplier<? extends BE> factory) {
			this.block = block;
			this.factory = factory;
		}

		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> withSimple(PEBlockCapability<CAP, CONTEXT> capability) {
			return withSimple(capability.lookup());
		}

		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> withSimple(BlockApiLookup<CAP, CONTEXT> capability) {
			return withSimple(capability, () -> true);
		}

		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> withSimple(PEBlockCapability<CAP, CONTEXT> capability, BooleanSupplier shouldApply) {
			return withSimple(capability.lookup(), shouldApply);
		}

		@SuppressWarnings("unchecked")
		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> withSimple(BlockApiLookup<CAP, CONTEXT> capability, BooleanSupplier shouldApply) {
			return with(capability, (BiFunction<? super BE, CONTEXT, CAP>) SIMPLE_PROVIDER, shouldApply);
		}

		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> with(BlockApiLookup<CAP, CONTEXT> capability,
				Function<BlockApiLookup<CAP, CONTEXT>, BiFunction<? super BE, CONTEXT, CAP>> provider) {
			return with(capability, provider.apply(capability));
		}

		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> with(PEBlockCapability<CAP, CONTEXT> capability, BiFunction<? super BE, CONTEXT, CAP> provider) {
			return with(capability.lookup(), provider);
		}

		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> with(BlockApiLookup<CAP, CONTEXT> capability, BiFunction<? super BE, CONTEXT, CAP> provider) {
			return with(capability, provider, () -> true);
		}

		/**
		 * @param shouldApply Determines whether the provider actually be attached to this block entity type. Useful for cases when we want to conditionally apply it
		 *                    based on loaded mods or a block's attributes.
		 */
		public <CAP, CONTEXT> BlockEntityTypeBuilder<BE> with(BlockApiLookup<CAP, CONTEXT> capability, BiFunction<? super BE, CONTEXT, CAP> provider,
				BooleanSupplier shouldApply) {
			capabilityProviders.add(new CapabilityData<>(capability, provider, shouldApply));
			return this;
		}

		public BlockEntityTypeBuilder<BE> without(BlockApiLookup<?, ?>... capabilities) {
			for (BlockApiLookup<?, ?> capability : capabilities) {
				capabilityProviders.removeIf(data -> data.capability() == capability);
			}
			return this;
		}

		public BlockEntityTypeBuilder<BE> without(Collection<? extends BlockApiLookup<?, ?>> capabilities) {
			capabilityProviders.removeIf(data -> capabilities.contains(data.capability()));
			return this;
		}

		public BlockEntityTypeBuilder<BE> clientTicker(BlockEntityTicker<BE> ticker) {
			Preconditions.checkState(clientTicker == null, "Client ticker may only be set once.");
			clientTicker = ticker;
			return this;
		}

		public BlockEntityTypeBuilder<BE> serverTicker(BlockEntityTicker<BE> ticker) {
			Preconditions.checkState(serverTicker == null, "Server ticker may only be set once.");
			serverTicker = ticker;
			return this;
		}

		public BlockEntityTypeBuilder<BE> commonTicker(BlockEntityTicker<BE> ticker) {
			return clientTicker(ticker)
					.serverTicker(ticker);
		}

		@SuppressWarnings("ConstantConditions")
		public BlockEntityTypeRegistryObject<BE> build() {
			//Note: There is no data fixer type as there is not a way of exposing data fixers to mods yet
			//26.3 removed BlockEntityType$Builder; the constructor now takes the supplier and the block set directly
			BlockEntityTypeRegistryObject<BE> holder = registerPE(block.getName(),
					() -> new BlockEntityType<>(factory, java.util.Set.of(block.getBlocks())));
			holder.tickers(clientTicker, serverTicker);
			holder.capabilities(capabilityProviders.isEmpty() ? null : capabilityProviders);
			return holder;
		}
	}
}
