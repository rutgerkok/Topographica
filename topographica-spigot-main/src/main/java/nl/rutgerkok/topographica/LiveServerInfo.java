package nl.rutgerkok.topographica;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;
import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.webserver.IntPair;
import nl.rutgerkok.topographica.webserver.ServerInfo;
import nl.rutgerkok.topographica.webserver.WebPlayer;
import nl.rutgerkok.topographica.webserver.WebWorld;

/**
 * This class is tricky to get right. The web server runs on another thread than
 * the Minecraft server. The Minecraft server is not thread-safe at all, so we
 * cannot call directly into it from the web server thread. Instead, we maintain
 * our own player list, and update the data on a regular basis.
 *
 * <p>
 * Updating of player data is both through events, and by polling. In this way,
 * updates can never be missed. Updating of world data is only done through
 * events.
 */
final class LiveServerInfo extends ServerInfo implements Listener {

    private class CachedPlayer implements WebPlayer {
        private final String displayName;
        private final long position;
        private final String worldName;
        private final byte updateTag;

        public CachedPlayer(Player player, byte updateTag) {
            this.displayName = ChatColor.stripColor(player.getDisplayName()).trim();
            Location location = player.getLocation();
            this.position = IntPair.toLong(location.getBlockX(), location.getBlockZ());
            this.worldName = location.getWorld().getName();
            this.updateTag = updateTag;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public long getPosition() {
            return position;
        }

    }

    private class CachedWorld implements WebWorld {
        private final String worldName;

        CachedWorld(World world) {
            this.worldName = world.getName();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CachedWorld other = (CachedWorld) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (!worldName.equals(other.worldName)) {
                return false;
            }
            return true;
        }

        @Override
        public String getDisplayName() {
            return config.getWorldConfig(worldName).getDisplayName();
        }

        @Override
        public String getFolderName() {
            return worldName;
        }

        private LiveServerInfo getOuterType() {
            return LiveServerInfo.this;
        }

        @Override
        public int hashCode() {
            return worldName.hashCode();
        }
    }

    private final Plugin plugin;
    private final Config config;

    private final ConcurrentMap<UUID, CachedPlayer> players = new ConcurrentHashMap<>(64, 0.75f, 1);
    private final Set<CachedWorld> worlds = Sets.newSetFromMap(new ConcurrentHashMap<CachedWorld, Boolean>());

    private Runnable playerPositionUpdater = new Runnable() {

        byte updateTag = 1;

        /**
         * In theory, PlayerJoinEvent/PlayerKickEvent/PlayerQuitEvent should
         * keep this list up-to-date. In practice, players can change
         * permissions while online, or events can be missed due to bugs in
         * Spigot. This routine updates all player positions, adds new players
         * and removes players that are offline.
         *
         * <p>
         * Note that this method must run on the server thread, so it is
         * impossible that a player is inserted mid-update. (The handler of
         * PlayerJoinEvent is the only other code that adds players to the list,
         * and that code is also always called on the server thread.)
         */
        @Override
        public void run() {

            // Update all online players
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!shouldRender(player)) {
                    continue;
                }
                players.put(player.getUniqueId(), new CachedPlayer(player, updateTag));
            }

            // Remove the ones that didn't get updated
            for (Iterator<CachedPlayer> it = players.values().iterator(); it.hasNext();) {
                CachedPlayer livePlayer = it.next();
                if (livePlayer.updateTag != updateTag) {
                    // Oops, player was not updated. Apparently he logged off,
                    // his permissions changed, walked outside the rendering
                    // area, etc.
                    it.remove();
                }
            }

            updateTag++;
        }


    };

    LiveServerInfo(Plugin plugin, Config config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");

        plugin.getServer().getScheduler().runTaskTimer(plugin, this.playerPositionUpdater, 1, 20 * 10);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Add worlds that already exist
        for (World world : plugin.getServer().getWorlds()) {
            this.worlds.add(new CachedWorld(world));
        }
    }


    @Override
    public Path getImagesFolder() {
        return config.getWebConfig().getImagesFolder();
    }

    @Override
    public Collection<? extends WebPlayer> getPlayers(WebWorld world) {
        String worldName = ((CachedWorld) world).worldName;
        ImmutableList.Builder<CachedPlayer> players = ImmutableList.builder();
        for (CachedPlayer player : this.players.values()) {
            if (player.worldName.equals(worldName)) {
                players.add(player);
            }
        }
        return players.build();
    }

    @Override
    public int getPort() {
        return config.getWebConfig().getPort();
    }

    @Override
    public Collection<? extends WebWorld> getWorlds() {
        return ImmutableSet.copyOf(worlds);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.shouldRender(player)) {
            this.players.put(player.getUniqueId(), new CachedPlayer(player, (byte) -1));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        this.players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Teleporting is usually over long distances
        // To make the map feel more snappy, we immediately update the player
        Player player = event.getPlayer();
        if (shouldRender(player, event.getTo())) {
            this.players.put(player.getUniqueId(), new CachedPlayer(player, (byte) -1));
        } else {
            this.players.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        this.worlds.add(new CachedWorld(event.getWorld()));
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        this.worlds.remove(new CachedWorld(event.getWorld()));
    }

    private boolean shouldRender(Player player) {
        return shouldRender(player, player.getLocation());
    }

    private boolean shouldRender(Player player, Location location) {
        return config.getConfig(player.getWorld()).shouldRenderColumn(location.getBlockX(), location.getBlockZ());
    }

}
