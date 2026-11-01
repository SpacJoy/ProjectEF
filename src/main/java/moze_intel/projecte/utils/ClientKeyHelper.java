package moze_intel.projecte.utils;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.InputConstants;
import moze_intel.projecte.network.PENetwork;
import moze_intel.projecte.network.packets.to_server.KeyPressPKT;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ClientKeyHelper {

	//26.3 throws when a Category id is registered twice, so the single instance is shared with EMCManagerClient
	public static final KeyMapping.Category PE_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("projecte", "main"));

	private static ImmutableMap<PEKeybind, KeyMapping> peToMc = ImmutableMap.of();

	/**
	 * Called by client mod initializer (stage 4) to register all ProjectEF key bindings.
	 * Uses Fabric KeyMappingHelper instead of NeoForge RegisterKeyMappingsEvent.
	 * KeyConflictContext and KeyModifier have been dropped — now uses pure vanilla KeyMapping.
	 */
	public static void registerKeyBindings() {
		ImmutableMap.Builder<PEKeybind, KeyMapping> builder = ImmutableMap.builder();
		//Fabric's vanilla KeyMapping has no modifier support and duplicate physical keys mask one another.
		//Register one armor key and dispatch X/Shift+X as boots/helmet below, matching the NeoForge behavior.
		KeyMapping armorToggle = registerKeyBinding(PEKeybind.BOOTS_TOGGLE, InputConstants.KEY_X);
		builder.put(PEKeybind.HELMET_TOGGLE, armorToggle);
		builder.put(PEKeybind.BOOTS_TOGGLE, armorToggle);
		addKeyBinding(builder, PEKeybind.CHARGE, InputConstants.KEY_V);
		addKeyBinding(builder, PEKeybind.EXTRA_FUNCTION, InputConstants.KEY_C);
		addKeyBinding(builder, PEKeybind.FIRE_PROJECTILE, InputConstants.KEY_R);
		addKeyBinding(builder, PEKeybind.MODE, InputConstants.KEY_G);
		peToMc = builder.build();
	}

	private static void addKeyBinding(ImmutableMap.Builder<PEKeybind, KeyMapping> builder, PEKeybind keyBind, int keyCode) {
		builder.put(keyBind, registerKeyBinding(keyBind, keyCode));
	}

	private static KeyMapping registerKeyBinding(PEKeybind keyBind, int keyCode) {
		KeyMapping keyMapping = new PEKeyMapping(keyBind, keyCode);
		KeyMappingHelper.registerKeyMapping(keyMapping);
		return keyMapping;
	}

	public static Component getKeyName(PEKeybind k) {
		KeyMapping keyMapping = peToMc.get(k);
		Component keyName = keyMapping == null ? TextComponentUtil.build(k) : keyMapping.getTranslatedKeyMessage();
		if (k == PEKeybind.HELMET_TOGGLE) {
			return Component.translatable("key.keyboard.left.shift").append(" + ").append(keyName);
		}
		return keyName;
	}

	private static class PEKeyMapping extends KeyMapping {

		private final PEKeybind keybind;
		private boolean lastState;

		PEKeyMapping(PEKeybind keybind, int keyCode) {
			//26.3: the category is a KeyMapping.Category record now, not a plain translation-key String.
			super(keybind.getTranslationKey(), InputConstants.Type.KEYBOARD, keyCode, PE_CATEGORY);
			this.keybind = keybind;
		}

		@Override
		public void setDown(boolean value) {
			super.setDown(value);
			boolean state = isDown();
			if (state != lastState) {
				if (state) {
					PEKeybind pressed = keybind == PEKeybind.BOOTS_TOGGLE && Minecraft.getInstance().hasShiftDown() ? PEKeybind.HELMET_TOGGLE : keybind;
					PENetwork.sendToServer(new KeyPressPKT(pressed));
				}
				lastState = state;
			}
		}
	}
}
