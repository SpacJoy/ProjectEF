package moze_intel.projecte.gameObjs.items.armor;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class GemFeet extends GemArmorBase {

	private static final boolean STEP_ASSIST_DEFAULT = false;

	private final Supplier<ItemAttributeModifiers> defaultModifiers;
	private final Supplier<ItemAttributeModifiers> defaultWithStepAssistModifiers;

	public GemFeet(Properties props) {
		super(ArmorType.BOOTS, props.component(PEDataComponentTypes.STEP_ASSIST.get(), STEP_ASSIST_DEFAULT));
		//26.3 removed Item#getDefaultAttributeModifiers; the base modifiers now live on the item default component map
		this.defaultModifiers = Suppliers.memoize(() -> components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).withModifierAdded(
				Attributes.MOVEMENT_SPEED,
				new AttributeModifier(PECore.rl("armor"), 1.0, Operation.ADD_MULTIPLIED_TOTAL),
				EquipmentSlotGroup.FEET
		));
		this.defaultWithStepAssistModifiers = Suppliers.memoize(() -> getDefaultAttributeModifiers().withModifierAdded(
				Attributes.STEP_HEIGHT,
				new AttributeModifier(PECore.rl("gem_step_assist"), 0.4, Operation.ADD_VALUE),
				EquipmentSlotGroup.FEET
		));
	}

	//26.3: Item no longer declares getDefaultAttributeModifiers, so this is a plain helper instead of an override
	private ItemAttributeModifiers getDefaultAttributeModifiers() {
		return this.defaultModifiers.get();
	}

	public static void toggleStepAssist(ItemStack boots, Player player) {
		boolean oldValue = isStepAssist(boots);
		boolean enabled = !oldValue;
		boots.set(PEDataComponentTypes.STEP_ASSIST.get(), enabled);
		if (boots.getItem() instanceof GemFeet gemFeet) {
			gemFeet.syncStepAssistModifiers(boots, enabled);
		}
		player.sendSystemMessage(getComponent(enabled));
	}

	private void syncStepAssistModifiers(ItemStack stack, boolean enabled) {
		ItemAttributeModifiers expected = enabled ? defaultWithStepAssistModifiers.get() : defaultModifiers.get();
		if (!expected.equals(stack.get(DataComponents.ATTRIBUTE_MODIFIERS))) {
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, expected);
		}
	}

	private void syncStepAssistModifiers(ItemStack stack) {
		syncStepAssistModifiers(stack, isStepAssist(stack));
	}

	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull net.minecraft.server.level.ServerLevel level, @NotNull Entity entity, @NotNull net.minecraft.world.entity.EquipmentSlot slot) {
		super.inventoryTick(stack, level, entity, slot);
		//26.3 only calls this on the server, so the jump assist lives in GemFeetClientTick on the client tick
		if (entity instanceof Player player && isEquipped(stack, player)) {
			syncStepAssistModifiers(stack);
			player.resetFallDistance();
		}
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		tooltip.accept(PELang.GEM_LORE_FEET.translate());
		tooltip.accept(PELang.STEP_ASSIST_PROMPT.translate(ClientKeyHelper.getKeyName(PEKeybind.BOOTS_TOGGLE)));
		tooltip.accept(getComponent(isStepAssist(stack)));
	}

	private static boolean isStepAssist(ItemStack stack) {
		return stack.getOrDefault(PEDataComponentTypes.STEP_ASSIST.get(), STEP_ASSIST_DEFAULT);
	}

	private static Component getComponent(boolean enabled) {
		if (enabled) {
			return PELang.STEP_ASSIST.translate(ChatFormatting.GREEN, PELang.GEM_ENABLED);
		}
		return PELang.STEP_ASSIST.translate(ChatFormatting.RED, PELang.GEM_DISABLED);
	}
}
