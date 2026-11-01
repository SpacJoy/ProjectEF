package moze_intel.projecte.events;

import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import moze_intel.projecte.api.item_handlers.ItemHandlerHelper;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.AlchemicalBag;
import moze_intel.projecte.gameObjs.items.armor.PEArmor;
import moze_intel.projecte.gameObjs.items.armor.PEArmor.ReductionInfo;
import moze_intel.projecte.impl.TransmutationOffline;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerEvents {

	public static void register() {
		// On death or return from end, sync to the client
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> syncData(newPlayer));
		// Sync to the client for "normal" interdimensional teleports (nether portal, etc.)
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> syncData(player));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerConnect(handler.player, server));
	}

	private static void syncData(ServerPlayer player) {
		IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
		if (knowledge != null) {
			knowledge.sync(player);
		}
		IAlchBagProvider bagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player);
		if (bagProvider != null) {
			bagProvider.syncAllBags(player);
		}
	}

	private static void onPlayerConnect(ServerPlayer player, MinecraftServer server) {
		//The player is loading fresh online data, drop any cached offline data (fake players never open a connection, so they don't get here)
		TransmutationOffline.clear(player.getUUID());
		PECore.debugLog("Clearing offline data cache in preparation to load online data");

		IKnowledgeProvider knowledge = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
		if (knowledge != null) {
			knowledge.sync(player);
			PlayerHelper.updateScore(player, PlayerHelper.SCOREBOARD_EMC, knowledge.getEmc());
		}

		IAlchBagProvider alchBagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player);
		if (alchBagProvider != null) {
			alchBagProvider.syncAllBags(player);
		}

		PECore.debugLog("Sent knowledge and bag data to {}", player.getName());

		if (ProjectEConfig.common.highAlchemistUUIDs.get().contains(player.getUUID().toString())) {
			Component joinMessage = PELang.HIGH_ALCHEMIST.translateColored(ChatFormatting.BLUE, ChatFormatting.GOLD, player.getDisplayName());
			server.getPlayerList().broadcastSystemMessage(joinMessage, false);
		}
	}

	/**
	 * Alchemical bag suction handling, called from the item entity pickup mixin.
	 *
	 * @return true if the pickup was handled by a bag and vanilla pickup handling should be skipped.
	 */
	public static boolean onItemPickup(ItemEntity itemEntity, Player player) {
		if (itemEntity.level().isClientSide() || itemEntity.hasPickUpDelay()) {
			return false;
		}
		ItemStack bag = AlchemicalBag.getFirstBagWithSuctionItem(player, player.getInventory().getNonEquipmentItems());
		if (!bag.isEmpty()) {
			IAlchBagProvider bagProvider = PECapabilities.ALCH_BAG_CAPABILITY.find(player);
			if (bagProvider != null) {
				ItemStack stack = itemEntity.getItem();
				IItemHandler handler = bagProvider.getBag(((AlchemicalBag) bag.getItem()).color);
				ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);

				int pickedUpCount = stack.getCount() - remainder.getCount();
				if (pickedUpCount > 0) {
					player.take(itemEntity, pickedUpCount);
					if (remainder.isEmpty()) {
						itemEntity.discard();
						//Update to the picked up count so that onItemPickup knows how much got picked up
						stack.setCount(pickedUpCount);
					}
					player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), pickedUpCount);
					player.onItemPickup(itemEntity);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Fire invulnerability from fire protecting gear, called from the invulnerability mixin.
	 */
	public static boolean isFireInvulnerable(ServerPlayer player, DamageSource source) {
		return source.is(DamageTypeTags.IS_FIRE) && TickEvents.shouldPlayerResistFire(player);
	}

	/**
	 * Gets the combined damage reduction of any worn ProjectEF armor pieces against the given source, called from the damage mixin.
	 */
	public static ReductionInfo getArmorReduction(LivingEntity entity, DamageSource source) {
		ReductionInfo reductionInfo = ReductionInfo.ZERO;
		//26.3 removed LivingEntity#getArmorSlots; iterate the armor equipment slot group instead
		for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
			ItemStack armorStack = entity.getItemBySlot(slot);
			if (armorStack.getItem() instanceof PEArmor armorItem) {
				//We return the max of this piece's base reduction (in relation to the full set),
				// and the max damage an item can absorb for a given source
				reductionInfo = reductionInfo.add(armorItem.getReductionInfo(source));
			}
		}
		return reductionInfo;
	}
}
