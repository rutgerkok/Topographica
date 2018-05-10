package nl.rutgerkok.topographica;

import java.net.BindException;
import java.util.Locale;

import nl.rutgerkok.topographica.command.CommandHandler;
import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.event.BlockListener;
import nl.rutgerkok.topographica.event.LogToPlayerSender;
import nl.rutgerkok.topographica.render.ServerRenderer;
import nl.rutgerkok.topographica.scheduler.Scheduler;
import nl.rutgerkok.topographica.util.StartupLog;
import nl.rutgerkok.topographica.webserver.WebServer;

import org.bukkit.plugin.java.JavaPlugin;

public class Topographica extends JavaPlugin {

    private WebServer webServer;
    private Scheduler scheduler;
    private ServerRenderer renderer;
    private Config config;

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
        if (webServer != null) {
            webServer.disable();
        }
        scheduler.stopAll();
    }

    @Override
    public void onEnable() {
        StartupLog startupLog = new StartupLog(getLogger());
        scheduler = new Scheduler(this);
        config = loadConfigs(startupLog);
        webServer = enableWebServer(startupLog);
        renderer = new ServerRenderer(scheduler, config);

        new LogToPlayerSender(startupLog, this).sendExistingWarnings().listenForNewPlayers();
        getServer().getPluginManager().registerEvents(new BlockListener(renderer), this);
        this.getCommand(this.getName().toLowerCase(Locale.ROOT)).setExecutor(new CommandHandler(renderer));
    }

}
