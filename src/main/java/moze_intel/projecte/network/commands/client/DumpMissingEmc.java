package moze_intel.projecte.network.commands.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashSet;
import java.util.Set;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.MappingConfig;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.emc.mappers.OreBlacklistMapper;
import moze_intel.projecte.emc.mappers.RawMaterialsBlacklistMapper;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;

public class DumpMissingEmc {

	private static final boolean SKIP_TOP = Boolean.parseBoolean(System.getProperties().getProperty("projecte.skip_top"));

	/**
	 * Registers the client-side "/projecteclient dumpmissingemc" command.
	 * Called by the client mod initializer (PECoreClient). Uses Fabric's client command API
	 * (FabricClientCommandSource) instead of the server-side CommandSourceStack.
	 */
	public static void registerClientCommand() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommands.literal("projecteclient").then(buildCommand())));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
		return ClientCommands.literal("dumpmissingemc")
				.then(ClientCommands.argument("skip_expected", BoolArgumentType.bool())
						.executes(ctx -> execute(ctx, BoolArgumentType.getBool(ctx, "skip_expected")))
				).executes(ctx -> execute(ctx, false));
	}

	private static boolean expectedMissing(FeatureFlagSet enabledFeatures, ItemInfo info) {
		Holder<Item> holder = info.getItem();
		if (holder.is(PETags.Items.IGNORE_MISSING_EMC)) {
			return true;
		}
		if (switch (holder.value()) {
			//Assume unbreakable blocks won't have an EMC value by default
			case BlockItem blockItem when blockItem.getBlock().defaultDestroyTime() == Block.INDESTRUCTIBLE -> true;
			case Tome tome when !ProjectEConfig.common.craftableTome.get() -> true;
			default -> false;
		}) {
			return true;
		} else if (FabricLoader.getInstance().isDevelopmentEnvironment() && SKIP_TOP &&
				   holder.unwrapKey().map(key -> key.identifier().getNamespace().equals("theoneprobe")).orElse(false)) {
			//Skip TOP items in dev
			return true;
		}
		if (MappingConfig.isEnabled(OreBlacklistMapper.INSTANCE)) {
			if (holder.is(ConventionalItemTags.ORES) || holder.value() == Items.GILDED_BLACKSTONE) {
				return true;
			}
		}
		if (MappingConfig.isEnabled(RawMaterialsBlacklistMapper.INSTANCE)) {
			if (holder.is(ConventionalItemTags.RAW_MATERIALS) || holder.is(ConventionalItemTags.STORAGE_BLOCKS_RAW_COPPER) ||
				holder.is(ConventionalItemTags.STORAGE_BLOCKS_RAW_IRON) || holder.is(ConventionalItemTags.STORAGE_BLOCKS_RAW_GOLD)) {
				return true;
			}
		}
		PotionContents potionContents = info.getOrNull(DataComponents.POTION_CONTENTS);
		return potionContents != null && potionContents.potion().isPresent() && potionContents.potion().get().is(PETags.Potions.IGNORE_MISSING_EMC);
	}

	private static int execute(CommandContext<FabricClientCommandSource> ctx, boolean skipExpectedMissing) {
		FabricClientCommandSource source = ctx.getSource();
		RegistryAccess registryAccess = source.registryAccess();
		Minecraft minecraft = source.getClient();
		FeatureFlagSet features = minecraft.getConnection() == null ? FeatureFlags.DEFAULT_FLAGS : minecraft.getConnection().enabledFeatures();
		//26.3 Registry#get returns an Optional holder; getValue hands back the tab itself
		CreativeModeTab tab = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB).getValue(CreativeModeTabs.SEARCH);
		if (tab == null || tab.getSearchTabDisplayItems().isEmpty()) {
			//If the search tab hasn't been initialized yet initialize it
			boolean hasPermissions = minecraft.options.operatorItemsTab().get();
			if (!hasPermissions) {
				LocalPlayer player = source.getPlayer();
				if (player != null) {
					hasPermissions = player.canUseGameMasterBlocks();
				} else {
					//26.3 turned permission levels into PermissionCheck/PermissionSet pairs
					hasPermissions = Commands.LEVEL_GAMEMASTERS.check(source.permissions());
				}
			}

			try {
				tab.buildContents(new CreativeModeTab.ItemDisplayParameters(features, hasPermissions, registryAccess));
			} catch (Exception ignored) {
				//We can't initialize yet for some reason, so we will just end up falling back to base items only
			}
		}

		Set<ItemInfo> missing = new HashSet<>();
		//26.3 made Registry non-iterable, so walk its stream instead
		for (Item item : registryAccess.lookupOrThrow(Registries.ITEM).stream().toList()) {
			//Skip air, and skip any items that are not currently enabled in the world
			if (item != Items.AIR && item.isEnabled(features)) {
				//Note: This is intentionally not using Item#getDefaultInstance as data component based variants should be based on the creative mode tabs
				ItemInfo itemInfo = ItemInfo.fromItem(item);
				if (skipExpectedMissing && expectedMissing(features, itemInfo)) {
					//Skip any items that we expected to be missing (for example ores)
					continue;
				}
				if (!IEMCProxy.INSTANCE.hasValue(itemInfo)) {
					//If the item doesn't have EMC add it to the list of items we haven't addressed yet
					missing.add(itemInfo);
				}
			}
		}
		//Check all items in the search tab to see if they have an EMC value (as they may have data component variants declared)
		for (ItemStack stack : tab.getSearchTabDisplayItems()) {
			if (!stack.isEmpty() && !stack.getComponentsPatch().isEmpty()) {
				//If the stack is not empty, and it has non defaulted components: see if any of the added variants have EMC
				ItemInfo itemInfo = ItemInfo.fromStack(stack);
				if (IEMCProxy.INSTANCE.hasValue(itemInfo)) {
					//If it does, remove the default variant from missing if it was missing
					missing.remove(itemInfo.itemOnly());
				} else if (!skipExpectedMissing || !expectedMissing(features, itemInfo)) {
					//If it doesn't, add it to the set of items that are missing an EMC value
					missing.add(itemInfo);
				}
			}
		}
		int missingCount = missing.size();
		if (missingCount == 0) {
			source.sendFeedback(PELang.DUMP_MISSING_EMC_NONE_MISSING.translate());
		} else {
			if (missingCount == 1) {
				source.sendFeedback(PELang.DUMP_MISSING_EMC_ONE_MISSING.translate());
			} else {
				source.sendFeedback(PELang.DUMP_MISSING_EMC_MULTIPLE_MISSING.translate(missingCount));
			}
			missing.stream()
					.map(ItemInfo::toString)
					.sorted()
					.forEach(PECore.LOGGER::info);
		}
		return missingCount;
	}
}
