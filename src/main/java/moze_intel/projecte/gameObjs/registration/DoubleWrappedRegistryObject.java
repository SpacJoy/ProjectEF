package moze_intel.projecte.gameObjs.registration;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class DoubleWrappedRegistryObject<PRIMARY_REGISTRY, PRIMARY extends PRIMARY_REGISTRY, SECONDARY_REGISTRY, SECONDARY extends SECONDARY_REGISTRY> implements INamedEntry {

	protected final PEDeferredHolder<PRIMARY_REGISTRY, PRIMARY> primaryRO;
	protected final PEDeferredHolder<SECONDARY_REGISTRY, SECONDARY> secondaryRO;

	public DoubleWrappedRegistryObject(PEDeferredHolder<PRIMARY_REGISTRY, PRIMARY> primaryRO, PEDeferredHolder<SECONDARY_REGISTRY, SECONDARY> secondaryRO) {
		this.primaryRO = primaryRO;
		this.secondaryRO = secondaryRO;
	}

	@NotNull
	public PRIMARY getPrimary() {
		return primaryRO.get();
	}

	@NotNull
	public SECONDARY getSecondary() {
		return secondaryRO.get();
	}

	@Override
	public Identifier getId() {
		return primaryRO.getId();
	}

	@Override
	public String getName() {
		return primaryRO.getId().getPath();
	}
}
