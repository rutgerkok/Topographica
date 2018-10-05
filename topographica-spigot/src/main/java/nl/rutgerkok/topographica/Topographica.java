package nl.rutgerkok.topographica;

import java.net.BindException;
import java.nio.file.Path;
import java.util.Locale;

import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.topographica.command.CommandHandler;
import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.event.BlockListener;
import nl.rutgerkok.topographica.event.LogToPlayerSender;
import nl.rutgerkok.topographica.render.ChunkQueuePersistance;
import nl.rutgerkok.topographica.render.ServerRenderer;
import nl.rutgerkok.topographica.scheduler.Scheduler;
import nl.rutgerkok.topographica.util.StartupLog;
import nl.rutgerkok.topographica.webserver.WebServer;

public class Topographica extends JavaPlugin {

    private WebServer webServer;
    private Scheduler scheduler;
    private ServerRenderer renderer;
    private Config config;
    private ChunkQueuePersistance chunkQueuePersistance;

    private WebServer enableWebServer(StartupLog startupLog) {
        if (!config.getWebConfig().isInternalServerEnabled()) {
            return null;
        }
        try {
            return new WebServer(new PluginBundledFiles(this), new LiveServerInfo(this, config), this.getLogger());
        } catch (BindException e) {
            startupLog.severe("**** FAILED TO BIND TO PORT **** \nPort " + config.getWebConfig().getPort()
                    + " is already in use. The map will not function.");
            return null;
        }
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
        chunkQueuePersistance.saveRegionQueue(renderer);
        if (webServer != null) {
            webServer.disable();
        }
        scheduler.stopAll();
    }

    @Override
    public void onEnable() {
        Path savedQueueFile = this.getDataFolder().toPath().resolve("pending_regions.txt");

        StartupLog startupLog = StartupLog.wrapping(getLogger());
        scheduler = new Scheduler(this);
        config = loadConfigs(startupLog);
        webServer = enableWebServer(startupLog);
        renderer = new ServerRenderer(scheduler, config);

        chunkQueuePersistance = new ChunkQueuePersistance(savedQueueFile, this.getLogger());
        chunkQueuePersistance.loadFromQueue(getServer(), renderer);
        new LogToPlayerSender(startupLog, this).sendExistingWarnings().listenForNewPlayers();
        getServer().getPluginManager().registerEvents(new BlockListener(renderer), this);
        this.getCommand(this.getName().toLowerCase(Locale.ROOT)).setExecutor(new CommandHandler(renderer));
    }

}
