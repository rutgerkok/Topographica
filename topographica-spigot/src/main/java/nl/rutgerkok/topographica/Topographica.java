package nl.rutgerkok.topographica;

import java.net.BindException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.topographica.command.CommandHandler;
import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.event.BlockListener;
import nl.rutgerkok.topographica.event.LogToPlayerSender;
import nl.rutgerkok.topographica.render.ChunkQueuePersistance;
import nl.rutgerkok.topographica.render.ServerDrawTask;
import nl.rutgerkok.topographica.render.ServerTaskList;
import nl.rutgerkok.topographica.util.ServerThreadGetter;
import nl.rutgerkok.topographica.util.StartupLog;
import nl.rutgerkok.topographica.webserver.ServerInfo;
import nl.rutgerkok.topographica.webserver.WebServer;
import nl.rutgerkok.topographica.webserver.WebWorld;

public class Topographica extends JavaPlugin {

    private WebServer webServer;
    private ServerTaskList serverTaskList;
    private Config config;
    private ChunkQueuePersistance chunkQueuePersistance;
    private LiveServerInfo serverInfo;
    private ServerDrawTask drawTask;

    private WebServer enableWebServer(StartupLog startupLog, Config config, ServerInfo serverInfo) {
        Objects.requireNonNull(startupLog, "startupLog");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(serverInfo, "serverInfo");

        if (!config.getWebConfig().isInternalServerEnabled()) {
            return null;
        }
        try {
            return new WebServer(new PluginBundledFiles(this), serverInfo, this.getLogger());
        } catch (BindException e) {
            startupLog.severe("**** FAILED TO BIND TO PORT **** \nPort " + config.getWebConfig().getPort()
                    + " is already in use. The map will not function.");
            return null;
        }
    }

    /**
     * Gets the web world, which contains information on how the rendered world
     * is presented on the web.
     *
     * @param world
     *            The Minecraft world.
     * @return The world, or empty if no map is rendered.
     */
    public Optional<WebWorld> getWebWorld(World world) {
        return this.serverInfo.getWorld(world);
    }

    private Config loadConfigs(StartupLog startupLog) {
        saveDefaultConfig();
        Config config = new Config(getServer(), getConfig(), getDataFolder().toPath(), startupLog);
        config.write(getConfig());
        saveConfig();
        return config;
    }

    @Override
    public void onDisable() {
        chunkQueuePersistance.saveRegionQueue(serverTaskList);
        drawTask.requestStop();
        if (webServer != null) {
            webServer.disable();
        }
    }

    @Override
    public void onEnable() {
        Path savedQueueFile = this.getDataFolder().toPath().resolve("pending_regions.txt");

        StartupLog startupLog = StartupLog.wrapping(getLogger());
        config = loadConfigs(startupLog);
        serverInfo = new LiveServerInfo(this, config);
        webServer = enableWebServer(startupLog, config, serverInfo);
        serverTaskList = new ServerTaskList(config);

        chunkQueuePersistance = new ChunkQueuePersistance(savedQueueFile, this.getLogger());
        chunkQueuePersistance.loadFromQueue(getServer(), serverTaskList);
        new LogToPlayerSender(startupLog, this).sendExistingWarnings().listenForNewPlayers();
        getServer().getPluginManager().registerEvents(new BlockListener(serverTaskList), this);
        this.getCommand(this.getName().toLowerCase(Locale.ROOT)).setExecutor(new CommandHandler(serverTaskList));

        ServerThreadGetter<Optional<ChunkSnapshot>> getter = callable -> {
            return getServer().getScheduler().callSyncMethod(this, callable);
        };
        drawTask = new ServerDrawTask(serverTaskList, getServer(), getter, config);
        this.getServer().getScheduler().runTaskAsynchronously(this, drawTask);
    }

}
