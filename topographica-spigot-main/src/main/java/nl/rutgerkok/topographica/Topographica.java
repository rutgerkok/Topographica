package nl.rutgerkok.topographica;

import java.net.BindException;
import java.util.Collection;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.render.ServerRenderer;
import nl.rutgerkok.topographica.render.WorldRenderer;
import nl.rutgerkok.topographica.scheduler.Scheduler;
import nl.rutgerkok.topographica.util.StartupLog;
import nl.rutgerkok.topographica.webserver.WebServer;

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
        if (args.length != 1) {
            return false;
        }
        switch (args[0]) {
            case "fullrender":
                World world = getWorld(sender);
                renderer.renderAllRegionsAsync(world);
                sender.sendMessage("Starting full render of world " + world.getName());
                return true;
            case "status":
                Collection<WorldRenderer> renderers = renderer.getActiveRenderers();
                sender.sendMessage("Active renderers:");
                if (renderers.isEmpty()) {
                    sender.sendMessage("No active renderers.");
                }
                for (WorldRenderer renderer : renderers) {
                    sender.sendMessage("* " + renderer.getWorld().getName() + ": " + renderer.getQueueSize()
                            + " regions in queue");
                }
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
        renderer = new ServerRenderer(scheduler, config);

        new LogToPlayerSender(startupLog, this).sendExistingWarnings().listenForNewPlayers();
    }

}
