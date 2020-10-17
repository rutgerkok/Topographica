package nl.rutgerkok.topographica.command;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

/**
 * Handler of a subcommand.
 *
 */
abstract class SubCommand {

    static final String ERROR_COLOR = ChatColor.DARK_RED.toString();
    static final String HEADER_COLOR = ChatColor.DARK_GREEN.toString();
    static final String MAIN_COLOR = ChatColor.GREEN.toString();

    protected static World getWorld(CommandSender sender) {
        if (sender instanceof Entity) {
            return ((Entity) sender).getWorld();
        }
        if (sender instanceof BlockCommandSender) {
            return ((BlockCommandSender) sender).getBlock().getWorld();
        }
        return sender.getServer().getWorlds().get(0);
    }

    protected static void requireMinMaxSize(List<?> list, int minSize, int maxSize) throws CommandUsageException {
        int size = list.size();
        if (size < minSize || size > maxSize) {
            throw new CommandUsageException();
        }
    }

    /**
     * Executes the command.
     *
     * @param sender
     *            The sender.
     * @param baseLabel
     *            The base label.
     * @param args
     *            The args. "/command foo bar baz" gives
     *            {@code args == ["bar", "baz"]}.
     * @throws CommandUsageException
     *             If the syntax of the command is invalid.
     */
    abstract void execute(CommandSender sender, String baseLabel, List<String> args) throws CommandUsageException;

    /**
     * Gets a very short description of this command.
     *
     * @return A description.
     */
    abstract String getDescription();

    /**
     * Gets the command syntax, for example "[world]" for a command that should
     * be called as "/command render [world]".
     *
     * @return The syntax string, may be empty.
     */
    abstract String getSyntax();

    /**
     * Asks for tab completions.
     *
     * @param sender
     *            The sender.
     * @param args
     *            The args. "/command foo bar baz" gives
     *            {@code args == ["bar", "baz"]}.
     * @return List of tab completions, or null to complete player names.
     */
    abstract List<String> tabComplete(CommandSender sender, List<String> args);

}
