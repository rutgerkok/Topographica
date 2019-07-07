package nl.rutgerkok.topographica.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

import nl.rutgerkok.topographica.render.ServerTaskList;
import nl.rutgerkok.topographica.render.WorldTaskList;

final class StatusCommand extends SubCommand {

    private final ServerTaskList serverRenderer;

    public StatusCommand(ServerTaskList serverRenderer) {
        this.serverRenderer = Objects.requireNonNull(serverRenderer, "serverRenderer");
    }

    @Override
    void execute(CommandSender sender, String baseLabel, List<String> args) throws CommandUsageException {
        requireMinMaxSize(args, 0, 0);
        Map<UUID, WorldTaskList> renderers = serverRenderer.getActiveTaskLists();
        sender.sendMessage(HEADER_COLOR + "Active renderers:");
        if (renderers.isEmpty()) {
            sender.sendMessage(MAIN_COLOR + "No active renderers.");
        }
        int i = 1;
        for (Entry<UUID, WorldTaskList> entry : renderers.entrySet()) {
            UUID worldId = entry.getKey();
            WorldTaskList taskList = entry.getValue();
            sender.sendMessage(MAIN_COLOR + i + ". World \"" + getWorldName(sender, worldId) + "\": "
                    + taskList.calculateRegionsInQueue() + " regions in queue");
            i++;
        }
    }

    @Override
    String getDescription() {
        return "views the status of the renderers";
    }

    @Override
    String getSyntax() {
        return "";
    }

    private String getWorldName(CommandSender sender, UUID worldId) {
        World world = sender.getServer().getWorld(worldId);
        if (world == null) {
            return worldId.toString();
        }
        return world.getName();
    }

    @Override
    List<String> tabComplete(CommandSender sender, List<String> args) {
        return Collections.emptyList();
    }

}
