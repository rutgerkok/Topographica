package nl.rutgerkok.topographica.command;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.topographica.render.ServerTaskList;
import nl.rutgerkok.topographica.util.Chat;
import nl.rutgerkok.topographica.util.RegionFilesFinder;

final class FullRenderCommand extends SubCommand {

    private final ServerTaskList serverRenderer;

    public FullRenderCommand(ServerTaskList serverRenderer) {
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

        renderWorldAsync(sender, world);
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

    private void renderWorldAsync(CommandSender sender, World world) {
        Plugin plugin = JavaPlugin.getProvidingPlugin(getClass());
        sender.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                RegionFilesFinder.getRegions(world)
                        .forEach(region -> serverRenderer.askToRenderRegion(world, region));
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to find regions in world " + world.getName(), e);
                sender.sendMessage(Chat.WARNING_COLOR + "Failed to find regions in world. " + e.getMessage());
            }
        });
    }

    @Override
    List<String> tabComplete(CommandSender sender, List<String> args) {
        return Collections.emptyList();
    }

}
