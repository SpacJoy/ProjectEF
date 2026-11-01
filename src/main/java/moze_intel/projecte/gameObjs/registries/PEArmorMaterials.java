package moze_intel.projecte.gameObjs.registries;

import java.util.EnumMap;
import java.util.Map;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.EnumMatterType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public class PEArmorMaterials {

	private PEArmorMaterials() {
	}

	/**
	 * Durability multiplier matching vanilla diamond gear.
	 */
	private static final int DIAMOND_DURABILITY = 33;
	private static final Map<ArmorType, Integer> DIAMOND_RESISTANCES = Util.make(new EnumMap<>(ArmorType.class), map -> {
		map.put(ArmorType.BOOTS, 3);
		map.put(ArmorType.LEGGINGS, 6);
		map.put(ArmorType.CHESTPLATE, 8);
		map.put(ArmorType.HELMET, 3);
		map.put(ArmorType.BODY, 11);
	});

	/**
	 * 26.3 turned {@link ArmorMaterial} into a plain data record and removed the {@code armor_material} registry: there is
	 * nothing left to register, so these are held as plain constants and consumed through {@code Item.Properties#humanoidArmor}.
	 * <p>
	 * Note: these must stay below {@link #DIAMOND_RESISTANCES}, otherwise the defense map is still null when they are built.
	 */
	public static final ArmorMaterial DARK_MATTER = create("dark_matter", EnumMatterType.DARK_MATTER.getEnchantmentValue(), 2F, 0.1F);
	public static final ArmorMaterial RED_MATTER = create("red_matter", EnumMatterType.RED_MATTER.getEnchantmentValue(), 2F, 0.2F);
	public static final ArmorMaterial GEM_ARMOR = create("gem_armor", EnumMatterType.RED_MATTER.getEnchantmentValue(), 2F, 0.25F);

	//Note: The enchantment value of our armor matches the one of the corresponding tools of the same matter tier,
	// as the armor is meant to be enchantable just like vanilla gear.
	private static ArmorMaterial create(String assetName, int enchantmentValue, float toughness, float knockbackResistance) {
		return new ArmorMaterial(
				DIAMOND_DURABILITY,
				DIAMOND_RESISTANCES,
				enchantmentValue,
				SoundEvents.ARMOR_EQUIP_NETHERITE,
				toughness,
				knockbackResistance,
				ItemTags.REPAIRS_NETHERITE_ARMOR,
				equipmentAsset(assetName)
		);
	}

	/**
	 * 26.3 resolves the worn-armor texture through {@code assets/<namespace>/equipment/<name>.json}, which in turn points at
	 * {@code textures/entity/equipment/<layer>/<texture>.png}. Pointing every material at {@link EquipmentAssets#DIAMOND} was
	 * only a placeholder, and it made all our armor render as the vanilla diamond set. Each material now ships its own asset
	 * with the matching textures.
	 */
	private static ResourceKey<EquipmentAsset> equipmentAsset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ProjectEAPI.PROJECTE_MODID, name));
	}
}
