package nl.rutgerkok.topographica.command;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.bukkit.command.CommandSender;

final class HelpCommand extends SubCommand {

    private final CommandHandler commandHandler;

    public HelpCommand(CommandHandler commandHandler) {
        this.commandHandler = Objects.requireNonNull(commandHandler, "commandHandler");
    }

    @Override
    void execute(CommandSender sender, String baseLabel, List<String> args) throws CommandUsageException {
        requireMinMaxSize(args, 0, 0);
        sender.sendMessage(HEADER_COLOR + "Available subcommands of " + baseLabel + ":");
        for (Entry<String, SubCommand> entry : this.commandHandler.getRegisteredCommands().entrySet()) {
            SubCommand sub = entry.getValue();
            if (sub == this) {
                continue;
            }
            String usage = sub.getSyntax() + " - " + sub.getDescription();
            sender.sendMessage(MAIN_COLOR + "* " + entry.getKey() + " " + usage.trim());
        }
    }

    @Override
    String getDescription() {
        return "Displays help.";
    }

    @Override
    String getSyntax() {
        return "";
    }

    @Override
    List<String> tabComplete(CommandSender sender, List<String> args) {
        return Collections.emptyList();
    }

}
