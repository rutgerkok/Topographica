package nl.rutgerkok.topographica;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.MoreExecutors;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.render.WorldRenderer;
import nl.rutgerkok.topographica.scheduler.Scheduler;
import nl.rutgerkok.topographica.util.StartupLog;
import nl.rutgerkok.topographica.webserver.WebServer;

public class Main extends JavaPlugin {

    private final WebServer webserver;
    private Scheduler scheduler;
    private StartupLog log;
    private Config config;

    public Main() {
        webserver = new WebServer();
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

    private Config loadConfigs() {
        saveDefaultConfig();
        Config config = new Config(getServer(), getConfig(), getDataFolder().toPath(), log);
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
        webserver.disable();
        scheduler.stopAll();
    }

    @Override
    public void onEnable() {
        log = new StartupLog(getLogger());
        scheduler = new Scheduler(this);
        config = loadConfigs();

        webserver.enable(config.getWebConfig(), log);
        new LogToPlayerSender(log, this).sendExistingWarnings();
    }

}
