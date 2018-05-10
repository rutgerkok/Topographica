package nl.rutgerkok.topographica.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nl.rutgerkok.topographica.render.ServerRenderer;

import com.google.common.collect.ImmutableMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public final class CommandHandler implements TabExecutor {

    private final Map<String, SubCommand> subHandlers;

    public CommandHandler(ServerRenderer serverRenderer) {
        this.subHandlers = ImmutableMap.<String, SubCommand>builder()
                .put("status", new StatusCommand(serverRenderer))
                .put("fullrender", new FullRenderCommand(serverRenderer))
                .put("help", new HelpCommand(this))
                .build();
    }

    /**
     * Gets an immutable map of all registered subcommands.
     *
     * @return The immutable map.
     */
    Map<String, SubCommand> getRegisteredCommands() {
        return this.subHandlers; // This map is immutable, so this is safe
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            args = new String[] { "help" };
        }
        if (sender instanceof Player) {
            label = "/" + label;
        }
        String commandName = args[0];
        SubCommand subCommand = subHandlers.get(commandName.toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sender.sendMessage(
                    SubCommand.ERROR_COLOR + "Command \"" + label + " " + commandName + "\" not found. Try \""
                            + label + " help\".");
            return true;
        }
        try {
            subCommand.execute(sender, label, Arrays.asList(args).subList(1, args.length));
        } catch (CommandUsageException e) {
            sender.sendMessage(SubCommand.ERROR_COLOR + "Invalid syntax. Use \"" + label + " "
                    + (commandName.toLowerCase(Locale.ROOT) + " " + subCommand.getSyntax()).trim() + "\".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], subHandlers.keySet(), new ArrayList<String>());
        }
        SubCommand subCommand = subHandlers.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            return Collections.emptyList();
        }
        return subCommand.tabComplete(sender, Arrays.asList(args).subList(1, args.length));
    }

}
