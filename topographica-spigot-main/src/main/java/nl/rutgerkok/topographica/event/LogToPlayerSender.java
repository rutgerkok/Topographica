package nl.rutgerkok.topographica.event;

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

public final class LogToPlayerSender implements Listener {

    private final StartupLog log;
    private final Plugin plugin;

    public LogToPlayerSender(StartupLog log, Plugin plugin) {
        this.log = Objects.requireNonNull(log, "log");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Registers an event handler that prints all error messages when an
     * operator joins.
     *
     * @return This, for chaining.
     */
    public LogToPlayerSender listenForNewPlayers() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        return this;
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
     *
     * @return This, for chaining.
     */
    public LogToPlayerSender sendExistingWarnings() {
        List<String> messages = log.getMessages();
        if (messages.isEmpty()) {
            return this;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendWarnings(player, messages);
        }
        return this;
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
