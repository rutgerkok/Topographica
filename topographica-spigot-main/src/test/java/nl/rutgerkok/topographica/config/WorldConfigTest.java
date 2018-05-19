package nl.rutgerkok.topographica.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import nl.rutgerkok.topographica.util.Region;
import nl.rutgerkok.topographica.util.StartupLog;

public class WorldConfigTest {

    @Test
    public void testRadius() {
        ConfigurationSection section = new YamlConfiguration();
        section.set("radius", 512);
        section.set("center-x", 176);
        section.set("center-z", 108);
        section.createSection("colors", ImmutableMap.of("AIR", "#000000"));
        WorldConfig config = new WorldConfig(null, "test", section, StartupLog.discarding());

        assertTrue(config.shouldRender(Region.of(0, 0)));
        assertTrue(config.shouldRender(Region.of(1, 0)));
        assertFalse(config.shouldRender(Region.of(10, 0)));

        assertTrue(config.shouldRenderChunk(11, -26));
    }

    @Test
    public void testSmall() {
        ConfigurationSection section = new YamlConfiguration();
        section.set("radius", 100);
        section.set("center-x", 176);
        section.set("center-z", 108);
        section.createSection("colors", ImmutableMap.of("AIR", "#000000"));
        WorldConfig config = new WorldConfig(null, "test", section, StartupLog.discarding());

        assertTrue(config.shouldRender(Region.of(0, 0)));
    }
}
