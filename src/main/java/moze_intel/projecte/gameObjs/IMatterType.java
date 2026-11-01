package moze_intel.projecte.gameObjs;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

/**
 * 26.3 deleted {@code net.minecraft.world.item.Tier}, and with it {@code DiggerItem} and every concrete tool class.
 * Tool stats are no longer read at runtime through an interface: they are baked into the {@code TOOL} and
 * {@code ATTRIBUTE_MODIFIERS} data components when the item is constructed, both derived from a {@link ToolMaterial}
 * record. So the matter numbers stay here and {@link #toToolMaterial()} adapts them to the record the vanilla
 * {@code Item.Properties#pickaxe}/{@code #axe}/{@code #shovel}/{@code #hoe}/{@code #sword} helpers expect.
 */
public interface IMatterType {

	float getChargeModifier();

	int getMatterTier();

	float getSpeed();

	float getAttackDamageBonus();

	int getEnchantmentValue();

	TagKey<Block> getIncorrectBlocksForDrops();

	/**
	 * The matter tools are indestructible: their {@code Item.Properties} subclass swallows the durability call, so the
	 * durability field here is only a codec placeholder and never reaches a stack.
	 */
	default ToolMaterial toToolMaterial() {
		return new ToolMaterial(getIncorrectBlocksForDrops(), 0, getSpeed(), getAttackDamageBonus(), getEnchantmentValue(),
				ItemTags.REPAIRS_NETHERITE_ARMOR);
	}

	default Item.Properties applyPickaxe(Item.Properties properties, float attackDamage, float attackSpeed) {
		return properties.pickaxe(toToolMaterial(), attackDamage, attackSpeed);
	}

	default Item.Properties applyAxe(Item.Properties properties, float attackDamage, float attackSpeed) {
		return properties.axe(toToolMaterial(), attackDamage, attackSpeed);
	}

	default Item.Properties applyShovel(Item.Properties properties, float attackDamage, float attackSpeed) {
		return properties.shovel(toToolMaterial(), attackDamage, attackSpeed);
	}

	default Item.Properties applyHoe(Item.Properties properties, float attackDamage, float attackSpeed) {
		return properties.hoe(toToolMaterial(), attackDamage, attackSpeed);
	}

	default Item.Properties applySword(Item.Properties properties, float attackDamage, float attackSpeed) {
		return properties.sword(toToolMaterial(), attackDamage, attackSpeed);
	}

	/**
	 * Hammer, katar and morning star mine block tags that no vanilla tool type describes, so they need the generic
	 * {@code tool(...)} overload; the last argument is the weapon's block-disabling duration, matching what the vanilla
	 * sword branch would pass.
	 */
	default Item.Properties applyCustomTool(Item.Properties properties, TagKey<Block> mineable, float attackDamage, float attackSpeed) {
		return properties.tool(toToolMaterial(), mineable, attackDamage, attackSpeed, 1.0F);
	}
}
