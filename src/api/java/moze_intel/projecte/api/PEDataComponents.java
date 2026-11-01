package moze_intel.projecte.api;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public class PEDataComponents {

	public static final DataComponentType<Integer> CHARGE = get("charge");

	private PEDataComponents() {
	}

	@SuppressWarnings("unchecked")
	private static <TYPE> DataComponentType<TYPE> get(String name) {
		//26.3 note: Registry#getOrThrow is a HolderGetter method and hands back a Holder.Reference, not the value.
		//Reading the component type itself needs getValueOrThrow, otherwise the cast below blows up.
		return (DataComponentType<TYPE>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValueOrThrow(
				ResourceKey.create(Registries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(ProjectEAPI.PROJECTE_MODID, name)));
	}
}