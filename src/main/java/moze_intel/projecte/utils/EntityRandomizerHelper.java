package moze_intel.projecte.utils;

import java.util.Optional;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.PETags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.rabbit.Rabbit.Variant;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class EntityRandomizerHelper {

	@Nullable
	public static Mob getRandomEntity(Level level, Mob toRandomize) {
		EntityType<?> entType = toRandomize.getType();
		boolean isPeaceful = entType.builtInRegistryHolder().is(PETags.Entities.RANDOMIZER_PEACEFUL);
		boolean isHostile = entType.builtInRegistryHolder().is(PETags.Entities.RANDOMIZER_HOSTILE);
		if (isPeaceful && isHostile) {
			//If it is in both lists do some extra checks to see if it really is peaceful
			// currently this only includes our special casing for killer rabbits
			if (toRandomize instanceof Rabbit rabbit && rabbit.getVariant() == Variant.EVIL) {
				//Killer rabbits are not peaceful
				isPeaceful = false;
			}
		}
		if (isPeaceful) {
			return createRandomEntity(level, toRandomize, PETags.Entities.RANDOMIZER_PEACEFUL);
		} else if (isHostile) {
			Mob ent = createRandomEntity(level, toRandomize, PETags.Entities.RANDOMIZER_HOSTILE);
			if (ent instanceof Rabbit rabbit) {
				//26.3 made Rabbit#setVariant private; the variant is applied through its data component instead
				rabbit.setComponent(net.minecraft.core.component.DataComponents.RABBIT_VARIANT, Variant.EVIL);
			}
			return ent;
		}
		return null;
	}

	@Nullable
	private static Mob createRandomEntity(Level level, Entity current, TagKey<EntityType<?>> type) {
		EntityType<?> currentType = current.getType();
		EntityType<?> newType = getRandomTagEntry(level.getRandom(), BuiltInRegistries.ENTITY_TYPE, type, currentType);
		if (currentType == newType) {
			//If the type is identical return null so that nothing happens
			return null;
		}
		//26.3 EntityType#create requires an EntitySpawnReason
		Entity newEntity = newType.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		if (newEntity instanceof Mob) {
			return (Mob) newEntity;
		} else if (newEntity != null) {
			//There are "invalid" entries in the list that do not correspond to, kill the new entity
			newEntity.discard();
			// and log a warning
			PECore.LOGGER.warn("Invalid Entity type {} in mob randomizer tag {}. All entities in this tag are expected to be a mob.",
					BuiltInRegistries.ENTITY_TYPE.getKey(newType), type.location());
		}
		return null;
	}

	private static EntityType<?> getRandomTagEntry(RandomSource random, Registry<EntityType<?>> registry, TagKey<EntityType<?>> tagKey, EntityType<?> toExclude) {
		Optional<HolderSet.Named<EntityType<?>>> optionalTag = ((net.minecraft.core.HolderGetter<net.minecraft.world.entity.EntityType<?>>) registry).get(tagKey);
		if (optionalTag.isEmpty()) {
			//Failed to get the tag
			return toExclude;
		}
		HolderSet.Named<EntityType<?>> tag = optionalTag.get();
		int size = tag.size();
		//26.3 dropped EntityType#is(TagKey); the built-in registry holder carries the tags now
		if (size == 0 || size == 1 && toExclude.builtInRegistryHolder().is(tagKey)) {
			return toExclude;
		}
		Optional<EntityType<?>> obj;
		do {
			obj = tag.getRandomElement(random)
					.map(Holder::value);
		} while (obj.isPresent() && obj.get().equals(toExclude));
		//Fallback to base
		return obj.orElse(toExclude);
	}
}