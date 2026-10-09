package moze_intel.projecte.network.packets.to_server;

import io.netty.buffer.ByteBuf;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.PEItemCapability;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.api.capabilities.item.IModeChanger;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.armor.GemArmorBase;
import moze_intel.projecte.gameObjs.items.armor.GemChest;
import moze_intel.projecte.gameObjs.items.armor.GemFeet;
import moze_intel.projecte.gameObjs.items.armor.GemHelmet;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.text.ILangEntry;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import moze_intel.projecte.network.PEPacketContext;
import org.jetbrains.annotations.NotNull;

public record KeyPressPKT(PEKeybind key) implements IPEPacket {

	public static final CustomPacketPayload.Type<KeyPressPKT> TYPE = new CustomPacketPayload.Type<>(PECore.rl("key_press"));
	public static final StreamCodec<ByteBuf, KeyPressPKT> STREAM_CODEC = PEKeybind.STREAM_CODEC.map(KeyPressPKT::new, KeyPressPKT::key);

	@NotNull
	@Override
	public CustomPacketPayload.Type<KeyPressPKT> type() {
		return TYPE;
	}

	@Override
	public void handle(PEPacketContext context) {
		Player player = context.player();
		if (player.isSpectator()) {
			return;
		}
		if (key == PEKeybind.HELMET_TOGGLE) {
			ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
			if (!helm.isEmpty() && helm.is(PEItems.GEM_HELMET.get())) {
				GemHelmet.toggleNightVision(helm, player);
			}
			return;
		} else if (key == PEKeybind.BOOTS_TOGGLE) {
			ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
			if (!boots.isEmpty() && boots.is(PEItems.GEM_BOOTS.get())) {
				GemFeet.toggleStepAssist(boots, player);
			}
			return;
		}
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			switch (key) {
				case CHARGE -> {
					if (tryPerformCapability(player, stack, hand, PECapabilities.CHARGE_ITEM_CAPABILITY, IItemCharge::changeCharge)) {
						return;
					} else if (hand == InteractionHand.MAIN_HAND && isSafe(stack) && GemArmorBase.hasAnyPiece(player)) {
						player.setAttached(PEAttachmentTypes.GEM_ARMOR_STATE, !player.getAttachedOrCreate(PEAttachmentTypes.GEM_ARMOR_STATE));
						ILangEntry langEntry = player.getAttachedOrCreate(PEAttachmentTypes.GEM_ARMOR_STATE) ? PELang.GEM_ACTIVATE : PELang.GEM_DEACTIVATE;
						player.sendSystemMessage(langEntry.translate());
						return;
					}
				}
				case EXTRA_FUNCTION -> {
					if (tryPerformCapability(player, stack, hand, PECapabilities.EXTRA_FUNCTION_ITEM_CAPABILITY, IExtraFunction::doExtraFunction)) {
						return;
					} else if (hand == InteractionHand.MAIN_HAND && isSafe(stack) && player.getAttachedOrCreate(PEAttachmentTypes.GEM_ARMOR_STATE)) {
						ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
						if (!chestplate.isEmpty() && chestplate.is(PEItems.GEM_CHESTPLATE.get()) &&
							PlayerHelper.checkCooldown(player, PEItems.GEM_CHESTPLATE.get(), ProjectEConfig.server.cooldown.player.gemChest)) {
							GemChest.doExplode(player);
							return;
						}
					}
				}
				case FIRE_PROJECTILE -> {
					if (!stack.isEmpty() && PlayerHelper.checkCooldown(player, stack.getItem(), ProjectEConfig.server.cooldown.player.projectile)
						&& tryPerformCapability(player, stack, hand, PECapabilities.PROJECTILE_SHOOTER_ITEM_CAPABILITY, IProjectileShooter::shootProjectile)) {
						PlayerHelper.swingItem(player, hand);
					}
					if (hand == InteractionHand.MAIN_HAND && isSafe(stack) && player.getAttachedOrCreate(PEAttachmentTypes.GEM_ARMOR_STATE)) {
						ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
						if (!helmet.isEmpty() && helmet.is(PEItems.GEM_HELMET.get())) {
							GemHelmet.doZap(player);
							return;
						}
					}
				}
				case MODE -> {
					if (tryPerformCapability(player, stack, hand, PECapabilities.MODE_CHANGER_ITEM_CAPABILITY, IModeChanger::changeMode)) {
						return;
					}
				}
			}
		}
		//Nothing in either hand handled the key. Fall back to a mode item worn in an accessory slot (a ring in its ring slot for example),
		// but only while the main hand cannot be doing something with that key itself.
		if (key == PEKeybind.MODE && mayFallBackFrom(player.getMainHandItem())) {
			changeWornMode(player);
		}
	}

	/**
	 * Whether the accessory fallback is allowed to react to the mode key while the given stack is in the main hand.
	 * <p>
	 * Only stacks whose behaviour is actually known may pass: an empty stack (which resolves to {@code minecraft:air}), a vanilla item, or
	 * one of our own items that has no mode of its own. Items from other mods are left alone, because they may well bind the same key for
	 * themselves, and vanilla only ever dispatches a physical key to a single binding.
	 */
	private static boolean mayFallBackFrom(ItemStack stack) {
		String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
		if (!namespace.equals(ResourceLocation.DEFAULT_NAMESPACE) && !namespace.equals(PECore.MODID)) {
			//Another mod's item: never override it
			return false;
		}
		//A held mode item is handled by the hand loop above; this guards the case where that reported nothing to do
		return PECapabilities.MODE_CHANGER_ITEM_CAPABILITY.find(stack) == null;
	}

	/**
	 * Changes the mode of the first accessory (Trinkets) item that exposes the mode changer capability. Worn stacks are live references that
	 * are not part of the player's container menu, so the slot has to be flagged afterwards or the change never reaches the client.
	 */
	private static void changeWornMode(Player player) {
		IItemHandler curios = IntegrationHelper.getCuriosInventory(player);
		if (curios == null) {
			return;
		}
		for (int i = 0, slots = curios.getSlots(); i < slots; i++) {
			ItemStack stack = curios.getStackInSlot(i);
			IModeChanger<?> changer = PECapabilities.MODE_CHANGER_ITEM_CAPABILITY.find(stack);
			//Pass a null hand: the stack is not in one of the player's hands
			if (changer != null && changer.changeMode(player, stack, null)) {
				curios.markSlotChanged(i);
				return;
			}
		}
	}

	private static <CAPABILITY> boolean tryPerformCapability(Player player, ItemStack stack, InteractionHand hand, PEItemCapability<CAPABILITY> capability,
			CapabilityProcessor<CAPABILITY> processor) {
		CAPABILITY impl = capability.find(stack);
		return impl != null && processor.process(impl, player, stack, hand);
	}

	private static boolean isSafe(ItemStack stack) {
		return ProjectEConfig.server.misc.unsafeKeyBinds.get() || stack.isEmpty();
	}

	@FunctionalInterface
	private interface CapabilityProcessor<CAPABILITY> {

		boolean process(CAPABILITY capability, Player player, ItemStack stack, InteractionHand hand);
	}
}
