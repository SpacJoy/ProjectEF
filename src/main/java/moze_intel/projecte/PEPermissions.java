package moze_intel.projecte;

import java.util.function.Predicate;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
//26.3 replaced the int permission levels with the PermissionLevel enum
import net.minecraft.server.permissions.PermissionLevel;

/**
 * Permission nodes for the ProjectEF commands, checked through the fabric permissions api (falling back to the given vanilla permission level when no permission
 * mod is installed).
 */
public class PEPermissions {

	//Commands
	public static final CommandPermissionNode COMMAND = new CommandPermissionNode(node("command"), PermissionLevel.ALL);

	public static final CommandPermissionNode COMMAND_REMOVE_EMC = nodeOpCommand("remove_emc");
	public static final CommandPermissionNode COMMAND_RESET_EMC = nodeOpCommand("reset_emc");
	public static final CommandPermissionNode COMMAND_SET_EMC = nodeOpCommand("set_emc");
	public static final CommandPermissionNode COMMAND_SHOW_BAG = nodeOpCommand("show_bag");
	public static final CommandPermissionNode COMMAND_EMC = nodeOpCommand("emc");
	public static final CommandPermissionNode COMMAND_EMC_ADD = nodeSubCommand(COMMAND_EMC, "add");
	public static final CommandPermissionNode COMMAND_EMC_REMOVE = nodeSubCommand(COMMAND_EMC, "remove");
	public static final CommandPermissionNode COMMAND_EMC_SET = nodeSubCommand(COMMAND_EMC, "set");
	public static final CommandPermissionNode COMMAND_EMC_TEST = nodeSubCommand(COMMAND_EMC, "test");
	public static final CommandPermissionNode COMMAND_EMC_GET = nodeSubCommand(COMMAND_EMC, "get");
	public static final CommandPermissionNode COMMAND_KNOWLEDGE = nodeOpCommand("knowledge");
	public static final CommandPermissionNode COMMAND_KNOWLEDGE_CLEAR = nodeSubCommand(COMMAND_KNOWLEDGE, "clear");
	public static final CommandPermissionNode COMMAND_KNOWLEDGE_LEARN = nodeSubCommand(COMMAND_KNOWLEDGE, "learn");
	public static final CommandPermissionNode COMMAND_KNOWLEDGE_UNLEARN = nodeSubCommand(COMMAND_KNOWLEDGE, "unlearn");
	public static final CommandPermissionNode COMMAND_KNOWLEDGE_TEST = nodeSubCommand(COMMAND_KNOWLEDGE, "test");

	private static String node(String nodeName) {
		return PECore.MODID + "." + nodeName;
	}

	private static CommandPermissionNode nodeOpCommand(String nodeName) {
		return new CommandPermissionNode(node("command." + nodeName), PermissionLevel.GAMEMASTERS);
	}

	private static CommandPermissionNode nodeSubCommand(CommandPermissionNode parent, String nodeName) {
		//Because sub commands can assume that the parent was checked before getting to them, permission mods can grant
		// the parent node; the sub node only exists in case someone wants to do more restricting
		return new CommandPermissionNode(parent.node() + "." + nodeName, parent.fallbackLevel());
	}

	public record CommandPermissionNode(String node, PermissionLevel fallbackLevel) implements Predicate<CommandSourceStack> {

		@Override
		public boolean test(CommandSourceStack source) {
			return Permissions.check(source, node, fallbackLevel);
		}
	}
}
