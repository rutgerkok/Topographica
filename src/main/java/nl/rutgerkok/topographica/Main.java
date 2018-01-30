package nl.rutgerkok.topographica;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import nl.rutgerkok.topographica.render.RawImage;
import nl.rutgerkok.topographica.render.RegionRenderer;

public class Main extends JavaPlugin {

    private final Webserver webserver;
    private final Executor workerThread = new Executor() {

        @Override
        public void execute(Runnable command) {
            getServer().getScheduler().runTaskAsynchronously(Main.this, command);
        }
    };

    public Main() {
        webserver = new Webserver();
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
                final RawImage image = new RawImage();
                new RegionRenderer(getWorld(sender), 0, 0).render(this, image).addListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Path output = getDataFolder().toPath().resolve("map.jpg");
                            image.outputAndReset(output);
                            sender.sendMessage("Finished writing map to " + output);
                        } catch (IOException e) {
                            sender.sendMessage("Error writing map: " + e.getMessage());
                            getLogger().log(Level.SEVERE, "Error writing map", e);
                        }
                    }
                }, workerThread);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDisable()
    {
        webserver.disable();
    }

    @Override
    public void onEnable() {
        webserver.enable();
        getDataFolder().mkdirs();
    }

}
