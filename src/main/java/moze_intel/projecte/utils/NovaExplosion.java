package moze_intel.projecte.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * ProjectE nova explosion.
 *
 * <p>26.3 turned {@link Explosion} into an interface and moved the real implementation into {@code ServerExplosion}, whose block interaction step is private and drops
 * every stack at the position the block used to occupy. ProjectE needs the opposite: collect every drop and spawn it once, next to whoever caused the explosion, and
 * hand the list of destroyed positions back to the caller so the client can render the particles. So this class implements the interface itself and re-implements the
 * three vanilla steps (positions, entity damage, block interaction) with the drop collection swapped out.
 */
public class NovaExplosion implements Explosion {

	private final ServerLevel level;
	@Nullable
	private final Entity source;
	private final DamageSource damageSource;
	private final ExplosionDamageCalculator damageCalculator;
	private final Vec3 center;
	private final float radius;
	private final boolean fire;
	private final Explosion.BlockInteraction blockInteraction;
	private final Map<Player, Vec3> hitPlayers = new HashMap<>();

	public NovaExplosion(ServerLevel level, @Nullable Entity source, Vec3 center, float radius, Explosion.BlockInteraction blockInteraction) {
		this(level, source, null, null, center, radius, false, blockInteraction);
	}

	public NovaExplosion(ServerLevel level, @Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator,
			Vec3 center, float radius, boolean fire, Explosion.BlockInteraction blockInteraction) {
		this.level = level;
		this.source = source;
		this.radius = radius;
		this.center = center;
		this.fire = fire;
		this.blockInteraction = blockInteraction;
		this.damageSource = damageSource == null ? level.damageSources().explosion(this) : damageSource;
		this.damageCalculator = damageCalculator == null ? makeDamageCalculator(source) : damageCalculator;
	}

	private static ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity source) {
		//[VanillaCopy] ServerExplosion#makeDamageCalculator
		return source == null ? new ExplosionDamageCalculator() : new EntityBasedExplosionDamageCalculator(source);
	}

	/**
	 * Runs the explosion and returns the positions the client should render particles at.
	 */
	public List<BlockPos> explode() {
		//[VanillaCopy] ServerExplosion#explode, but the block step is swapped for the drop collecting one below
		level.gameEvent(source, GameEvent.EXPLODE, center);
		List<BlockPos> toBlow = calculateExplodedPositions();
		hurtEntities();
		List<BlockPos> particlePositions = List.of();
		if (blockInteraction != Explosion.BlockInteraction.KEEP) {
			ProfilerFiller profiler = Profiler.get();
			profiler.push("explosion_blocks");
			particlePositions = interactWithBlocks(toBlow);
			profiler.pop();
		}
		if (fire) {
			createFire(toBlow);
		}
		return particlePositions;
	}

	//[VanillaCopy] ServerExplosion#calculateExplodedPositions
	private List<BlockPos> calculateExplodedPositions() {
		Set<BlockPos> set = new HashSet<>();
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				for (int k = 0; k < 16; k++) {
					if (i != 0 && i != 15 && j != 0 && j != 15 && k != 0 && k != 15) {
						continue;
					}
					double dirX = (float) i / 15.0F * 2.0 - 1.0;
					double dirY = (float) j / 15.0F * 2.0 - 1.0;
					double dirZ = (float) k / 15.0F * 2.0 - 1.0;
					double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
					dirX /= length;
					dirY /= length;
					dirZ /= length;
					float power = radius * (0.7F + level.getRandom().nextFloat() * 0.6F);
					double x = center.x;
					double y = center.y;
					double z = center.z;
					for (; power > 0.0F; power -= 0.22500001F) {
						BlockPos pos = BlockPos.containing(x, y, z);
						BlockState state = level.getBlockState(pos);
						FluidState fluidState = level.getFluidState(pos);
						if (!level.isInWorldBounds(pos)) {
							break;
						}
						Optional<Float> resistance = damageCalculator.getBlockExplosionResistance(this, level, pos, state, fluidState);
						if (resistance.isPresent()) {
							power -= (resistance.get() + 0.3F) * 0.3F;
						}
						if (power > 0.0F && damageCalculator.shouldBlockExplode(this, level, pos, state, power)) {
							set.add(pos);
						}
						x += dirX * 0.30000001192092896D;
						y += dirY * 0.30000001192092896D;
						z += dirZ * 0.30000001192092896D;
					}
				}
			}
		}
		return new ArrayList<>(set);
	}

	//[VanillaCopy] ServerExplosion#hurtEntities
	private void hurtEntities() {
		if (radius < 1.0E-5F) {
			return;
		}
		float diameter = radius * 2.0F;
		int minX = Mth.floor(center.x - (double) diameter - 1.0D);
		int maxX = Mth.floor(center.x + (double) diameter + 1.0D);
		int minY = Mth.floor(center.y - (double) diameter - 1.0D);
		int maxY = Mth.floor(center.y + (double) diameter + 1.0D);
		int minZ = Mth.floor(center.z - (double) diameter - 1.0D);
		int maxZ = Mth.floor(center.z + (double) diameter + 1.0D);
		List<Entity> entities = level.getEntities(source, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
		for (Entity entity : entities) {
			if (entity.ignoreExplosion(this)) {
				continue;
			}
			double distanceRatio = Math.sqrt(entity.distanceToSqr(center)) / (double) diameter;
			if (distanceRatio > 1.0D) {
				continue;
			}
			Vec3 lookFrom = entity instanceof PrimedTnt ? entity.position() : entity.getEyePosition();
			Vec3 direction = lookFrom.subtract(center).normalize();
			boolean shouldDamage = damageCalculator.shouldDamageEntity(this, entity);
			float knockbackMultiplier = damageCalculator.getKnockbackMultiplier(entity);
			float seenPercent = shouldDamage || knockbackMultiplier != 0.0F ? getSeenPercent(center, entity) : 0.0F;
			if (shouldDamage && (source == null || source.doTeamsAllowDamage(entity))) {
				entity.hurtServer(level, damageSource, damageCalculator.getEntityDamageAmount(this, entity, seenPercent));
			}
			double knockbackResistance = entity instanceof LivingEntity living ? living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) : 0.0D;
			double knockback = (1.0D - distanceRatio) * (double) seenPercent * (double) knockbackMultiplier * (1.0D - knockbackResistance);
			Vec3 push = direction.scale(knockback);
			entity.pushFromExplosion(push);
			if (entity.is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
				projectile.setOwner(damageSource.getEntity());
			} else if (entity instanceof Player player && !player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
				hitPlayers.put(player, push);
			}
			entity.onExplosionHit(source);
		}
	}

	/**
	 * ProjectE variant of {@code ServerExplosion#interactWithBlocks}: instead of dropping every stack where its block was, the drops are gathered into one list and
	 * spawned together by the caller. Returns the positions of the blocks that were not air, so the client can render particles for exactly those.
	 */
	private List<BlockPos> interactWithBlocks(List<BlockPos> toBlow) {
		List<BlockPos> particlePositions = new ArrayList<>();
		Util.shuffle(toBlow, level.getRandom());
		NonNullList<ItemStack> allDrops = NonNullList.create();
		for (BlockPos pos : toBlow) {
			BlockState state = level.getBlockState(pos);
			if (!state.isAir()) {
				particlePositions.add(pos);
			}
			state.onExplosionHit(level, pos, this, (stack, position) -> allDrops.add(stack));
		}
		LivingEntity placer = getIndirectSourceEntity();
		WorldHelper.createLootDrop(allDrops, level, placer == null ? center : placer.position());
		return particlePositions;
	}

	//[VanillaCopy] ServerExplosion#createFire
	private void createFire(List<BlockPos> toBlow) {
		for (BlockPos pos : toBlow) {
			if (level.getRandom().nextInt(3) == 0 && level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender()) {
				level.setBlockAndUpdate(pos, net.minecraft.world.level.block.BaseFireBlock.getState(level, pos));
			}
		}
	}

	//[VanillaCopy] ServerExplosion#getSeenPercent
	public static float getSeenPercent(Vec3 explosionCenter, Entity entity) {
		AABB box = entity.getBoundingBox();
		double stepX = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
		double stepY = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
		double stepZ = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
		double offsetX = (1.0D - Math.floor(1.0D / stepX) * stepX) / 2.0D;
		double offsetZ = (1.0D - Math.floor(1.0D / stepZ) * stepZ) / 2.0D;
		if (stepX < 0.0D || stepY < 0.0D || stepZ < 0.0D) {
			return 0.0F;
		}
		int misses = 0;
		int total = 0;
		for (double dx = 0.0D; dx <= 1.0D; dx += stepX) {
			for (double dy = 0.0D; dy <= 1.0D; dy += stepY) {
				for (double dz = 0.0D; dz <= 1.0D; dz += stepZ) {
					double x = Mth.lerp(dx, box.minX, box.maxX);
					double y = Mth.lerp(dy, box.minY, box.maxY);
					double z = Mth.lerp(dz, box.minZ, box.maxZ);
					Vec3 from = new Vec3(x + offsetX, y, z + offsetZ);
					if (entity.level().clip(new ClipContext(from, explosionCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() == HitResult.Type.MISS) {
						misses++;
					}
					total++;
				}
			}
		}
		return (float) misses / (float) total;
	}

	@Override
	public ServerLevel level() {
		return level;
	}

	@Override
	public Explosion.BlockInteraction getBlockInteraction() {
		return blockInteraction;
	}

	@Nullable
	@Override
	public LivingEntity getIndirectSourceEntity() {
		return Explosion.getIndirectSourceEntity(source);
	}

	@Nullable
	@Override
	public Entity getDirectSourceEntity() {
		return source;
	}

	@Override
	public float radius() {
		return radius;
	}

	@Override
	public Vec3 center() {
		return center;
	}

	@Override
	public boolean canTriggerBlocks() {
		return blockInteraction == Explosion.BlockInteraction.TRIGGER_BLOCK && source != null && source.is(net.minecraft.world.entity.EntityTypes.BREEZE_WIND_CHARGE)
				&& level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
	}

	@Override
	public boolean shouldAffectBlocklikeEntities() {
		return blockInteraction != Explosion.BlockInteraction.KEEP;
	}

	public DamageSource getDamageSource() {
		return damageSource;
	}

	public Map<Player, Vec3> getHitPlayers() {
		return hitPlayers;
	}

	/**
	 * Unused, kept so {@link Level} based callers that expect a plain {@link Explosion} still have a stable reference point.
	 */
	public Level getLevel() {
		return level;
	}
}
