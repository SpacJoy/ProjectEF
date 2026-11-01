package moze_intel.projecte.integration.trinkets;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Trinkets specific bridge. This class is only classloaded once the trinkets mod is confirmed present (the calls are
 * guarded in {@link moze_intel.projecte.integration.IntegrationHelper}), so ProjectE keeps loading fine without trinkets.
 * <p>
 * 26.3 note: Trinkets was rewritten by Patbox. The api package moved from {@code dev.emi.trinkets.api} to
 * {@code eu.pb4.trinkets.api}, {@code TrinketComponent} is now {@link TrinketAttachment} (reached through
 * {@link TrinketsApi#getAttachment}), and per-item registration goes through {@link TrinketCallback#setCallback}
 * instead of the removed {@code TrinketsApi#registerTrinket}.
 */
public final class TrinketsIntegration {

	private static final ProjectETrinket PROJECTE_TRINKET = new ProjectETrinket();

	private TrinketsIntegration() {
	}

	public static void registerTrinket(Item item) {
		TrinketCallback.setCallback(item, PROJECTE_TRINKET);
	}

	@Nullable
	public static IItemHandler getInventory(Player player) {
		TrinketAttachment attachment = TrinketsApi.getAttachment(player);
		if (attachment == null) {
			return null;
		}
		//The attachment already flattens groups, so unlike the old TrinketComponent#getInventory this is a single map.
		List<TrinketInventory> inventories = new ArrayList<>(attachment.getInventories().values());
		return inventories.isEmpty() ? null : new TrinketItemHandler(inventories);
	}
}
