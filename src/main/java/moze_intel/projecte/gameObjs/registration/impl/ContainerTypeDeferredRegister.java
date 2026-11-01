package moze_intel.projecte.gameObjs.registration.impl;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import java.util.function.Supplier;
import moze_intel.projecte.gameObjs.registration.INamedEntry;
import moze_intel.projecte.gameObjs.registration.PEDeferredRegister;
import moze_intel.projecte.utils.WorldHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class ContainerTypeDeferredRegister extends PEDeferredRegister<MenuType<?>> {

	public ContainerTypeDeferredRegister(String modid) {
		super(Registries.MENU, modid, ContainerTypeRegistryObject::new);
	}

	public <CONTAINER extends AbstractContainerMenu, BE extends BlockEntity> ContainerTypeRegistryObject<CONTAINER> register(INamedEntry nameProvider,
			Class<BE> blockEntityClass, IBlockEntityContainerFactory<CONTAINER, BE> factory) {
		return register(nameProvider, (id, inv, buf) -> factory.create(id, inv, getBlockEntityFromBuf(buf, blockEntityClass)));
	}

	public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER> register(INamedEntry nameProvider, IContainerFactory<CONTAINER> factory) {
		return register(nameProvider.getName(), factory);
	}

	public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER> register(String name, IContainerFactory<CONTAINER> factory) {
		return registerMenu(name, () -> new ExtendedMenuType<>((id, inv, data) -> factory.create(id, inv, wrapClientData(data)), RawScreenData.STREAM_CODEC));
	}

	@SuppressWarnings("unchecked")
	public <CONTAINER extends AbstractContainerMenu> ContainerTypeRegistryObject<CONTAINER> registerMenu(String name, Supplier<MenuType<CONTAINER>> supplier) {
		return (ContainerTypeRegistryObject<CONTAINER>) super.register(name, supplier);
	}

	private static <BE extends BlockEntity> BE getBlockEntityFromBuf(RegistryFriendlyByteBuf buf, Class<BE> type) {
		if (buf == null) {
			throw new IllegalArgumentException("Null packet buffer");
		} else if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			throw new UnsupportedOperationException("This method is only supported on the client.");
		}
		return ClientHelper.getBlockEntityFromBuf(buf, type);
	}

	@Nullable
	private static RegistryFriendlyByteBuf wrapClientData(RawScreenData data) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
			throw new UnsupportedOperationException("Menu extra data can only be read on the client.");
		}
		return ClientHelper.wrapClientData(data);
	}

	/**
	 * Serializes the given writer's output so that it can be shipped through the fabric screen handler api to the client menu factory.
	 */
	public static RawScreenData createScreenData(RegistryAccess registryAccess, Consumer<RegistryFriendlyByteBuf> writer) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
		try {
			writer.accept(buf);
			byte[] bytes = new byte[buf.readableBytes()];
			buf.readBytes(bytes);
			return new RawScreenData(bytes);
		} finally {
			buf.release();
		}
	}

	/**
	 * Nested class so that client only classes are never touched on a dedicated server (classes are only loaded on first access).
	 */
	private static class ClientHelper {

		private static <BE extends BlockEntity> BE getBlockEntityFromBuf(RegistryFriendlyByteBuf buf, Class<BE> type) {
			BlockPos pos = buf.readBlockPos();
			BE blockEntity = WorldHelper.getBlockEntity(type, Minecraft.getInstance().level, pos);
			if (blockEntity == null) {
				throw new IllegalStateException("Client could not locate block entity at " + pos + " for block entity container. "
												+ "This is likely caused by a mod breaking client side block entity lookup");
			}
			return blockEntity;
		}

		@Nullable
		private static RegistryFriendlyByteBuf wrapClientData(RawScreenData data) {
			RegistryAccess registryAccess = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
			if (registryAccess == null) {
				return null;
			}
			return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data.data()), registryAccess);
		}
	}

	/**
	 * Raw payload carrying the extra menu opening data, mirroring the classic "write to the buffer, read on the client" container flow.
	 */
	public record RawScreenData(byte[] data) {

		public static final StreamCodec<io.netty.buffer.ByteBuf, RawScreenData> STREAM_CODEC = ByteBufCodecs.BYTE_ARRAY.map(RawScreenData::new, RawScreenData::data);

		public static final RawScreenData EMPTY = new RawScreenData(new byte[0]);
	}

	/**
	 * Menu factory that additionally receives the extra data written when the menu was opened, may be null when opened without extra data.
	 */
	@FunctionalInterface
	public interface IContainerFactory<CONTAINER extends AbstractContainerMenu> {

		CONTAINER create(int id, Inventory inv, @Nullable RegistryFriendlyByteBuf buf);
	}

	@FunctionalInterface
	public interface IBlockEntityContainerFactory<CONTAINER extends AbstractContainerMenu, BE extends BlockEntity> {

		CONTAINER create(int id, Inventory inv, BE blockEntity);
	}
}
