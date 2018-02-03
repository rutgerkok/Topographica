package nl.rutgerkok.topographica;

import java.net.BindException;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.WebConfig;
import nl.rutgerkok.topographica.render.WorldRenderer;
import nl.rutgerkok.topographica.scheduler.Scheduler;
import nl.rutgerkok.topographica.util.StartupLog;
import nl.rutgerkok.topographica.webserver.WebServer;

import com.google.common.util.concurrent.MoreExecutors;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class Topographica extends JavaPlugin {

    private WebServer webServer;
    private Scheduler scheduler;
    private Config config;

    private WebServer enableWebServer(StartupLog startupLog) {
        WebConfig config = this.config.getWebConfig();
        if (!config.isInternalServerEnabled()) {
            return null;
        }
        try {
            return new WebServer(new PluginBundledFiles(this), config, this.getLogger());
        } catch (BindException e) {
            startupLog.severe("**** FAILED TO BIND TO PORT **** \nPort " + config.getPort()
                    + " is already in use. The map will not function.");
            return null;
        }
    }

    private World getWorld(CommandSender sender) {
        if (sender instanceof Entity) {
            return ((Entity) sender).getWorld();
        }
        if (sender instanceof BlockCommandSender) {
            return ((BlockCommandSender) sender).getBlock().getWorld();
        }
        return getServer().getWorlds().get(0);
    }

    private Config loadConfigs(StartupLog startupLog) {
        saveDefaultConfig();
        Config config = new Config(getServer(), getConfig(), getDataFolder().toPath(), startupLog);
        config.write(getConfig());
        saveConfig();
        return config;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName()) {
            case "render":
                final World world = getWorld(sender);
                scheduler.submitAll(new WorldRenderer(world, config)).addListener(new Runnable() {

                    @Override
                    public void run() {
                        sender.sendMessage("Done rendering " + world.getName());
                    }
                }, MoreExecutors.directExecutor());
                sender.sendMessage("Starting full render of world " + world.getName());
                return true;
            default:
                return false;
        }
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

        new LogToPlayerSender(startupLog, this).sendExistingWarnings().listenForNewPlayers();
    }

}
