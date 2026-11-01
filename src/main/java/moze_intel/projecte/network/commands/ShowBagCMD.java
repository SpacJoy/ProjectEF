package moze_intel.projecte.network.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import moze_intel.projecte.utils.OpenScreenHelper;
import moze_intel.projecte.PEPermissions;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.item_handlers.IItemHandlerModifiable;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.registries.PEAttachmentTypes;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.impl.capability.AlchBagImpl.AlchemicalBagAttachment;
import moze_intel.projecte.utils.text.PELang;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;

public class ShowBagCMD {

	private static final SimpleCommandExceptionType NOT_FOUND = new SimpleCommandExceptionType(PELang.SHOWBAG_NOT_FOUND.translate());
	private static final SuggestionProvider<CommandSourceStack> DYE_COLOR_SUGGESTIONS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Arrays.stream(DyeColor.values()).map(c -> c.getName().toLowerCase(Locale.ROOT)), builder);

	public static LiteralArgumentBuilder<CommandSourceStack> register(CommandBuildContext context) {
		return Commands.literal("showbag")
				.requires(PEPermissions.COMMAND_SHOW_BAG)
				.then(Commands.argument("color", StringArgumentType.word())
						.suggests(DYE_COLOR_SUGGESTIONS)
						.then(Commands.argument("target", EntityArgument.player())
								.executes(ctx -> showBag(ctx, getDyeColor(ctx, "color"), EntityArgument.getPlayer(ctx, "target"))))
						.then(Commands.argument("uuid", UuidArgument.uuid())
								.executes(ctx -> showBag(ctx, getDyeColor(ctx, "color"), UuidArgument.getUuid(ctx, "uuid")))));
	}

	private static DyeColor getDyeColor(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
		String colorName = StringArgumentType.getString(ctx, name);
		DyeColor color = DyeColor.byName(colorName, null);
		if (color == null) {
			throw NOT_FOUND.create();
		}
		return color;
	}

	private static int showBag(CommandContext<CommandSourceStack> ctx, DyeColor color, ServerPlayer player) throws CommandSyntaxException {
		ServerPlayer senderPlayer = ctx.getSource().getPlayerOrException();
		return showBag(senderPlayer, createContainer(senderPlayer, player, color));
	}

	private static int showBag(CommandContext<CommandSourceStack> ctx, DyeColor color, UUID uuid) throws CommandSyntaxException {
		ServerPlayer senderPlayer = ctx.getSource().getPlayerOrException();
		return showBag(senderPlayer, createContainer(ctx.getSource().getServer(), senderPlayer, uuid, color));
	}

	private static int showBag(ServerPlayer senderPlayer, MenuProvider container) {
		OpenScreenHelper.openMenuWithData(senderPlayer, container, b -> {
			b.writeBoolean(false);
			b.writeBoolean(false);
		});
		return Command.SINGLE_SUCCESS;
	}

	private static MenuProvider createContainer(ServerPlayer sender, ServerPlayer target, DyeColor color) {
		IItemHandlerModifiable inv = (IItemHandlerModifiable) Objects.requireNonNull(PECapabilities.ALCH_BAG_CAPABILITY.find(target)).getBag(color);
		Component name = PELang.SHOWBAG_NAMED.translate(PEItems.getBag(color), target.getDisplayName());
		return getContainer(sender, name, inv, false, () -> target.isAlive() && !target.hasDisconnected());
	}

	private static MenuProvider createContainer(MinecraftServer server, ServerPlayer sender, UUID target, DyeColor color) throws CommandSyntaxException {
		//Try to get the bag
		IItemHandlerModifiable inv = loadOfflineBag(server, target, color);
		//26.3 removed Item#getDescription; the plain item name comes off a default stack instead
		Component name = PEItems.getBag(color).getName(PEItems.getBagReference(color).asStack());
		//26.3 removed MinecraftServer#getProfileCache; profiles are resolved through the services bundle now
		Optional<GameProfile> profileByUUID = server.services().profileResolver().fetchById(target);
		if (profileByUUID.isPresent()) {
			//If we have a cache of the player, include their last known name in the name of the bag
			name = PELang.SHOWBAG_NAMED.translate(name, profileByUUID.get().name());
		}
		return getContainer(sender, name, inv, true, () -> true);
	}

	private static MenuProvider getContainer(ServerPlayer sender, Component name, IItemHandlerModifiable inv, boolean immutable,
			BooleanSupplier canInteractWith) {
		return new MenuProvider() {
			@NotNull
			@Override
			public Component getDisplayName() {
				return name;
			}

			@Override
			public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInv, @NotNull Player player) {
				//Note: Selected is unused for offhand
				return new AlchBagContainer(windowId, sender.getInventory(), InteractionHand.OFF_HAND, inv, 0, immutable) {
					@Override
					public boolean stillValid(@NotNull Player player) {
						return canInteractWith.getAsBoolean();
					}
				};
			}
		};
	}

	private static IItemHandlerModifiable loadOfflineBag(MinecraftServer server, UUID playerUUID, DyeColor color) throws CommandSyntaxException {
		Path player = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerUUID.toString() + ".dat");
		if (Files.exists(player) && Files.isRegularFile(player)) {
			try (InputStream in = Files.newInputStream(player)) {
				CompoundTag playerDat = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
				//26.3 dropped the type-checking overload of CompoundTag#contains, and getCompound now returns an Optional
				Optional<CompoundTag> attachmentData = playerDat.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
				if (attachmentData.isPresent()) {
					CompoundTag bagData = attachmentData.get().getCompoundOrEmpty(PEAttachmentTypes.ALCHEMICAL_BAGS.identifier().toString());
					RegistryOps<Tag> serializationContext = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
					DataResult<AlchemicalBagAttachment> result = AlchemicalBagAttachment.CODEC.parse(serializationContext, bagData);
					if (result.isSuccess()) {
						return result.getOrThrow().getBag(color);
					}
				}
			} catch (IOException e) {
				// fall through to below
			}
		}
		throw NOT_FOUND.create();
	}
}
