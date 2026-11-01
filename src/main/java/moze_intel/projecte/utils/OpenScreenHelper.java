package moze_intel.projecte.utils;

import java.util.function.Consumer;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister.RawScreenData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper utilities for opening extended screen handlers on Fabric, where the vanilla {@code openMenu} does not support writing extra data.
 */
public final class OpenScreenHelper {

	private OpenScreenHelper() {
	}

	/**
	 * Opens a menu and writes the given block position as extra opening data.
	 */
	public static void openMenuAt(Player player, MenuProvider provider, BlockPos pos) {
		player.openMenu(wrapWithScreenData(player, provider, buf -> buf.writeBlockPos(pos)));
	}

	/**
	 * Opens a menu and writes custom data using the given writer.
	 */
	public static void openMenuWithData(Player player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
		player.openMenu(wrapWithScreenData(player, provider, writer));
	}

	/**
	 * Opens a menu with no extra data.
	 */
	public static void openMenuSimple(Player player, MenuProvider provider) {
		player.openMenu(wrapWithScreenData(player, provider, buf -> {}));
	}

	/**
	 * Wraps a vanilla MenuProvider into an ExtendedMenuProvider that passes extra data through the Fabric screen handler API.
	 */
	private static ExtendedMenuProvider<RawScreenData> wrapWithScreenData(Player player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
		return new ExtendedMenuProvider<>() {
			@Override
			public @NotNull Component getDisplayName() {
				return provider.getDisplayName();
			}

			@Override
			public @NotNull AbstractContainerMenu createMenu(int syncId, @NotNull Inventory playerInventory, @NotNull Player p) {
				return provider.createMenu(syncId, playerInventory, p);
			}

			@Override
			public @NotNull RawScreenData getScreenOpeningData(@NotNull ServerPlayer serverPlayer) {
				return ContainerTypeDeferredRegister.createScreenData(serverPlayer.registryAccess(), writer);
			}
		};
	}
}
