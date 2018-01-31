package nl.rutgerkok.topographica;

import java.util.concurrent.Executor;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.render.WorldRenderer;
import nl.rutgerkok.topographica.util.Logg;
import nl.rutgerkok.topographica.webserver.WebServer;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private final WebServer webserver;
    private final Executor workerThread = new Executor() {

        @Override
        public void execute(Runnable command) {
            getServer().getScheduler().runTaskAsynchronously(Main.this, command);
        }
    };
    private Logg log;
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

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName()) {
            case "render":
                final World world = getWorld(sender);
                WorldRenderer.renderWorld(this, world, config).addListener(new Runnable() {

                    @Override
                    public void run() {
                        sender.sendMessage("Done rendering " + world.getName() + "!");
                    }
                }, workerThread);
                sender.sendMessage("Starting full render of world " + world.getName());
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDisable() {
        webserver.disable();
    }

    @Override
    public void onEnable() {
        log = new Logg(getLogger());

        saveDefaultConfig();
        config = new Config(getServer(), getConfig(), log);
        config.write(getConfig());
        saveConfig();

        webserver.enable(config.getWebConfig(), log);
        getDataFolder().mkdirs();
    }

}
