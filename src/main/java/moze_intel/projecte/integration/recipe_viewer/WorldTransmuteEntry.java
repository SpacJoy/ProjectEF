package moze_intel.projecte.integration.recipe_viewer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.world_transmutation.IWorldTransmutation;
import moze_intel.projecte.api.world_transmutation.SimpleWorldTransmutation;
import moze_intel.projecte.api.world_transmutation.WorldTransmutation;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public record WorldTransmuteEntry(Either<ItemStack, FluidInfo> input, Either<ItemStack, FluidInfo> output, @Nullable Either<ItemStack, FluidInfo> altOutput) {

	//26.3 removed ItemStack.SINGLE_ITEM_CODEC in favour of the plain CODEC
	private static final Codec<Either<ItemStack, FluidInfo>> EITHER_CODEC = Codec.either(ItemStack.CODEC, FluidInfo.BUCKET_CODEC);
	public static final Codec<WorldTransmuteEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			EITHER_CODEC.fieldOf("input").forGetter(WorldTransmuteEntry::input),
			EITHER_CODEC.fieldOf("output").forGetter(WorldTransmuteEntry::output),
			EITHER_CODEC.optionalFieldOf("alt_output").forGetter(entry -> Optional.ofNullable(entry.altOutput()))
	).apply(instance, (input, output, altOutput) -> new WorldTransmuteEntry(input, output, altOutput.orElse(null))));

	public Identifier syntheticId() {
		String name = stripForSynthetic(input) + "/" + stripForSynthetic(output);
		if (altOutput != null) {
			name += "/" + stripForSynthetic(altOutput);
		}
		return PECore.rl("/world_transmutation/" + name + "/");
	}

	private String stripForSynthetic(Either<ItemStack, FluidInfo> either) {
		//26.3 renamed ItemStack#getItemHolder to typeHolder
		return RecipeViewerHelper.stripForSynthetic(either.map(ItemStack::typeHolder, FluidInfo::holder));
	}

	private static boolean equals(@Nullable Either<ItemStack, FluidInfo> a, @Nullable Either<ItemStack, FluidInfo> b) {
		if (a == b) {
			return true;
		} else if (a == null || b == null) {
			return false;
		}
		Optional<ItemStack> leftA = a.left();
		Optional<ItemStack> leftB = b.left();
		if (leftA.isPresent() != leftB.isPresent()) {
			return false;
		} else if (leftA.isPresent()) {
			return ItemStack.isSameItemSameComponents(leftA.get(), leftB.get());
		}
		//Note: These should always be present, but use orElse just to avoid the warning of get without isPresent check
		return a.right().map(FluidInfo::fluid).orElse(Fluids.EMPTY) == b.right().map(FluidInfo::fluid).orElse(Fluids.EMPTY);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WorldTransmuteEntry other = (WorldTransmuteEntry) o;
		return equals(input, other.input) && equals(output, other.output) && equals(altOutput, other.altOutput);
	}

	private static int hash(Either<ItemStack, FluidInfo> either) {
		return either.map(ItemStack::hashItemAndComponents, info -> info.fluid().hashCode());
	}

	@Override
	public int hashCode() {
		int hash = hash(input);
		hash = 31 * hash + hash(output);
		if (altOutput != null) {
			hash = 31 * hash + hash(altOutput);
		}
		return hash;
	}

	@Nullable
	public static WorldTransmuteEntry create(IWorldTransmutation transmutation) {
		Either<ItemStack, FluidInfo> input, output, altOutput;
		if (transmutation instanceof SimpleWorldTransmutation(Holder<Block> origin, Holder<Block> result, Holder<Block> altResult)) {
			input = createInfo(origin.value());
			output = createInfo(result.value());
			altOutput = transmutation.hasAlternate() ? createInfo(altResult.value()) : null;
		} else if (transmutation instanceof WorldTransmutation(BlockState originState, BlockState result, BlockState altResult)) {
			input = createInfo(originState);
			output = createInfo(result);
			altOutput = transmutation.hasAlternate() ? createInfo(altResult) : null;
		} else {
			throw new IllegalStateException("Unknown transmutation implementation: " + transmutation);
		}
		return input == null || output == null ? null : new WorldTransmuteEntry(input, output, altOutput);
	}

	@Nullable
	private static <STATE> Either<ItemStack, FluidInfo> createInfo(STATE state, Function<STATE, Block> blockGetter, Function<STATE, ItemStack> itemGetter) {
		if (blockGetter.apply(state) instanceof LiquidBlock liquidBlock && liquidBlock.fluid != Fluids.EMPTY) {
			return Either.right(FluidInfo.bucket(liquidBlock.fluid));
		}
		ItemStack item = itemGetter.apply(state);
		return item.isEmpty() ? null : Either.left(item);
	}

	@Nullable
	private static Either<ItemStack, FluidInfo> createInfo(Block block) {
		return createInfo(block, Function.identity(), ItemStack::new);
	}

	@Nullable
	private static Either<ItemStack, FluidInfo> createInfo(BlockState state) {
		return createInfo(state, BlockStateBase::getBlock, WorldTransmuteEntry::itemFromBlock);
	}

	private static ItemStack itemFromBlock(BlockState state) {
		try {
			//We don't have a world or position, but try pick block anyways
			//26.3 moved getCloneItemStack onto the block state, and the signature no longer takes the BlockState
			return state.getCloneItemStack(null, null, true);
		} catch (Exception e) {
			//It failed, probably because of the null world and pos
			return new ItemStack(state.getBlock());
		}
	}
}
