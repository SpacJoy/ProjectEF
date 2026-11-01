package moze_intel.projecte.gameObjs.customRecipes;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PERecipeConditions;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

public class TomeEnabledCondition implements ResourceCondition {

	public static final TomeEnabledCondition INSTANCE = new TomeEnabledCondition();
	public static final MapCodec<TomeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

	private TomeEnabledCondition() {
	}

	@Override
	public ResourceConditionType<?> getType() {
		return PERecipeConditions.TOME_ENABLED;
	}

	@Override
	//26.3 changed ResourceCondition#test to take a RegistryOps.RegistryInfoLookup
	public boolean test(RegistryOps.RegistryInfoLookup registryLookup) {
		return ProjectEConfig.common.craftableTome.get();
	}
}
