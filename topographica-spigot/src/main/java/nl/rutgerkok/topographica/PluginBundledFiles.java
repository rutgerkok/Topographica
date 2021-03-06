package nl.rutgerkok.topographica;

import java.io.InputStream;
import java.util.Objects;

import org.bukkit.plugin.Plugin;

import nl.rutgerkok.topographica.webserver.BundledFiles;

/**
 * Wraps a plugin to make it conform to the {@link BundledFiles} interface.
 *
 */
final class PluginBundledFiles implements BundledFiles {

    private final Plugin plugin;

    public PluginBundledFiles(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public InputStream getResource(String string) {
        return plugin.getResource(string);
    }

}
