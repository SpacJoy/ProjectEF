package moze_intel.projecte.gameObjs.items.tools;
import net.minecraft.server.level.ServerLevel;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes;
import moze_intel.projecte.gameObjs.items.IHasConditionalAttributes.AttributeCollector;
import moze_intel.projecte.gameObjs.items.IItemMode;
import moze_intel.projecte.gameObjs.items.IModeEnum;
import moze_intel.projecte.gameObjs.items.tools.PEKatar.KatarMode;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.ToolHelper;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class PEKatar extends PETool implements IItemMode<KatarMode>, IExtraFunction, IHasConditionalAttributes {

	public PEKatar(IMatterType matterType, int numCharges, Properties props) {
		super(matterType, numCharges, matterType.applyCustomTool(props, PETags.Blocks.MINEABLE_WITH_PE_KATAR, 27, -2.4F)
				.component(PEDataComponentTypes.KATAR_MODE.get(), KatarMode.SLAY_HOSTILE)
		);
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull net.minecraft.world.item.component.TooltipDisplay display, @NotNull java.util.function.Consumer<Component> tooltip, @NotNull TooltipFlag flags) {
		super.appendHoverText(stack, context, display, tooltip, flags);
		tooltip.accept(getToolTip(stack));
	}

	@NotNull

	@Override
	protected float getShortCutDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
		float destroySpeed = super.getShortCutDestroySpeed(stack, state);
		if (destroySpeed == 1) {
			//Special handling for swords which still have hardcoded material checks
			// Note: we don't bother with the cobweb check because that will get caught by the tag for the blocks we can mine,
			// but we do need to include the material based checks that vanilla's sword still has
			if (state.is(BlockTags.SWORD_EFFICIENT)) {
				return 1.5F;
			}
		}
		return destroySpeed;
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockState blockState = level.getBlockState(context.getClickedPos());
		//Order that it attempts to use the item:
		// Strip logs, hoe ground, carve pumpkin, shear beehive, AOE remove logs, AOE remove leaves
		return ToolHelper.performActions(context, blockState, ToolHelper.stripLogsAOE(context, blockState, 0),
				(ctx, state) -> ToolHelper.scrapeAOE(ctx, state, 0),
				(ctx, state) -> ToolHelper.waxOffAOE(ctx, state, 0),
				(ctx, state) -> ToolHelper.tillAOE(ctx, state, 0),
				(ctx, state) -> {
					if (state.is(BlockTags.LOGS)) {
						//Mass clear (acting as an axe)
						//Note: We already tried to strip the log in an earlier action
						return ToolHelper.clearTagAOE(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getItemInHand(), 0, BlockTags.LOGS);
					}
					return InteractionResult.PASS;
				}, (ctx, state) -> {
					if (state.is(BlockTags.LEAVES)) {
						//Mass clear (acting as shears)
						return ToolHelper.clearTagAOE(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), ctx.getItemInHand(), 0, BlockTags.LEAVES);
					}
					return InteractionResult.PASS;
				});
	}

	@Override
	//26.3 changed Item#hurtEnemy to return void
	public void hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity damaged, @NotNull LivingEntity damager) {
		ToolHelper.attackWithCharge(stack, damaged, damager, 1.0F);
	}

	@NotNull
	@Override
	public InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		//Shear entities
		return ToolHelper.shearEntityAOE(player, hand, 0);
	}

	@Override
	public boolean doExtraFunction(@NotNull Player player, @NotNull ItemStack stack, InteractionHand hand) {
		if (player.getAttackStrengthScale(0F) == 1) {
			ToolHelper.attackAOE(stack, player, getMode(stack) == KatarMode.SLAY_ALL, ProjectEConfig.server.difficulty.katarDeathAura.get(), 0, hand);
			PlayerHelper.resetCooldown(player);
			return true;
		}
		return false;
	}

	@NotNull
	@Override
	public ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
		return ItemUseAnimation.BLOCK;
	}

	@Override
	public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
		return 72_000;
	}

	@Override
	public void adjustAttributes(ItemStack stack, AttributeCollector collector) {
		ToolHelper.applyChargeAttributes(stack, collector);
	}

	/**
	 * Copy of {@link net.minecraft.world.item.ShearsItem#interactLivingEntity(ItemStack, Player, LivingEntity, InteractionHand)}
	 */
	@NotNull
	@Override
	public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity entity, @NotNull InteractionHand hand) {
		if (entity instanceof Shearable target) {
			Level level = entity.level();
			if (target.readyForShearing()) {
				if (!level.isClientSide()) {
					//26.3: Shearable#shear now needs the server level and the tool stack
					target.shear((ServerLevel) level, SoundSource.PLAYERS, stack);
				}
				return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public DataComponentType<KatarMode> getDataComponentType() {
		return PEDataComponentTypes.KATAR_MODE.get();
	}

	@Override
	public KatarMode getDefaultMode() {
		return KatarMode.SLAY_HOSTILE;
	}

	public enum KatarMode implements IModeEnum<KatarMode> {
		SLAY_HOSTILE(PELang.MODE_KATAR_1),
		SLAY_ALL(PELang.MODE_KATAR_2);

		public static final Codec<KatarMode> CODEC = StringRepresentable.fromEnum(KatarMode::values);
		public static final IntFunction<KatarMode> BY_ID = ByIdMap.continuous(KatarMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
		public static final StreamCodec<ByteBuf, KatarMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, KatarMode::ordinal);

		private final IHasTranslationKey langEntry;
		private final String serializedName;

		KatarMode(IHasTranslationKey langEntry) {
			this.serializedName = name().toLowerCase(Locale.ROOT);
			this.langEntry = langEntry;
		}

		@NotNull
		@Override
		public String getSerializedName() {
			return serializedName;
		}

		@Override
		public String getTranslationKey() {
			return langEntry.getTranslationKey();
		}

		@Override
		public KatarMode next(ItemStack stack) {
			return switch (this) {
				case SLAY_HOSTILE -> SLAY_ALL;
				case SLAY_ALL -> SLAY_HOSTILE;
			};
		}
	}
}
