package moze_intel.projecte.api;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class PESounds {

	public static final Holder<SoundEvent> WIND = get("windmagic");
	public static final Holder<SoundEvent> WATER = get("watermagic");
	public static final Holder<SoundEvent> POWER = get("power");
	public static final Holder<SoundEvent> HEAL = get("heal");
	public static final Holder<SoundEvent> DESTRUCT = get("destruct");
	public static final Holder<SoundEvent> CHARGE = get("charge");
	public static final Holder<SoundEvent> UNCHARGE = get("uncharge");
	public static final Holder<SoundEvent> TRANSMUTE = get("transmute");

	private PESounds() {
	}

	private static Holder<SoundEvent> get(String name) {
		// 26.3: Registry#getHolderOrThrow is gone; look the holder up by id instead.
		return BuiltInRegistries.SOUND_EVENT.get(Identifier.fromNamespaceAndPath(ProjectEAPI.PROJECTE_MODID, name)).orElseThrow();
	}
}