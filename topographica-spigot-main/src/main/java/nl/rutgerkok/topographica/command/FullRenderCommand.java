package nl.rutgerkok.topographica.command;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import nl.rutgerkok.topographica.render.ServerRenderer;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

final class FullRenderCommand extends SubCommand {

    private final ServerRenderer serverRenderer;

    public FullRenderCommand(ServerRenderer serverRenderer) {
        this.serverRenderer = Objects.requireNonNull(serverRenderer, "serverRenderer");
    }

    @Override
    void execute(CommandSender sender, String baseLabel, List<String> args) throws CommandUsageException {
        requireMinMaxSize(args, 0, 1);
        World world = getWorld(sender);
        if (args.size() == 1) {
            world = sender.getServer().getWorld(args.get(0));
            if (world == null) {
                sender.sendMessage(ERROR_COLOR + "World \"" + args.get(0) + "\" does not exist.");
                return;
            }
        }
        serverRenderer.renderAllRegionsAsync(world);
        sender.sendMessage("Starting full render of world " + world.getName() + ". Use the \"" + baseLabel
                + " status\" command to keep track of the progress.");
    }

    @Override
    String getDescription() {
        return "fully (re-)renders a world.";
    }

    @Override
    String getSyntax() {
        return "[worldname]";
    }

    @Override
    List<String> tabComplete(CommandSender sender, List<String> args) {
        return Collections.emptyList();
    }

}
