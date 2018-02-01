package nl.rutgerkok.topographica;

import java.util.List;
import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import nl.rutgerkok.topographica.util.Chat;
import nl.rutgerkok.topographica.util.Permissions;
import nl.rutgerkok.topographica.util.StartupLog;

final class LogToPlayerSender implements Listener {

    private final StartupLog log;
    private final Plugin plugin;

    LogToPlayerSender(StartupLog log, Plugin plugin) {
        this.log = Objects.requireNonNull(log, "log");
        this.plugin = Objects.requireNonNull(plugin, "plugin");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        List<String> messages = log.getMessages();
        if (!messages.isEmpty()) {
            Player player = event.getPlayer();
            sendWarnings(player, messages);
        }
    }

    /**
     * Sends all warnings to players that are already online. (Useful after
     * plugin reloads.)
     */
    void sendExistingWarnings() {
        List<String> messages = log.getMessages();
        if (messages.isEmpty()) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendWarnings(player, messages);
        }
    }

    private void sendWarnings(Player player, List<String> messages) {
        if (!player.hasPermission(Permissions.ADMIN)) {
            return;
        }
        player.sendMessage(Chat.WARNING_COLOR + "There were errors during " + plugin.getName() + " startup.");
        for (String message : messages) {
            player.sendMessage(Chat.WARNING_COLOR + "* " + message);
        }
    }
}
