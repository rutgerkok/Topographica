package nl.rutgerkok.topographica.command;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import nl.rutgerkok.topographica.render.ServerRenderer;
import nl.rutgerkok.topographica.render.WorldRenderer;

import org.bukkit.command.CommandSender;

final class StatusCommand extends SubCommand {

    private final ServerRenderer serverRenderer;

    public StatusCommand(ServerRenderer serverRenderer) {
        this.serverRenderer = Objects.requireNonNull(serverRenderer, "serverRenderer");
    }

    @Override
    void execute(CommandSender sender, String baseLabel, List<String> args) throws CommandUsageException {
        requireMinMaxSize(args, 0, 0);
        List<WorldRenderer> renderers = serverRenderer.getActiveRenderers();
        sender.sendMessage(HEADER_COLOR + "Active renderers:");
        if (renderers.isEmpty()) {
            sender.sendMessage(MAIN_COLOR + "No active renderers.");
        }
        for (int i = 0; i < renderers.size(); i++) {
            WorldRenderer renderer = renderers.get(i);
            sender.sendMessage(MAIN_COLOR + (i + 1) + ". World \"" + renderer.getWorld().getName() + "\": "
                    + renderer.getQueueSize() + " regions in queue");
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

    @Override
    List<String> tabComplete(CommandSender sender, List<String> args) {
        return Collections.emptyList();
    }

}
