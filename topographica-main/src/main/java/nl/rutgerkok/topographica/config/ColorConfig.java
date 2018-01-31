package nl.rutgerkok.topographica.config;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.Logg;

public class ColorConfig {

    private static String encodeColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color parseColor(String name) {
        // Try color code (#ff00aa)
        try {
            if (name.startsWith("#") && name.length() == 7) {
                return Color.fromRGB(
                        Integer.valueOf(name.substring(1, 3), 16),
                        Integer.valueOf(name.substring(3, 5), 16),
                        Integer.valueOf(name.substring(5, 7), 16));
            }
        } catch (NumberFormatException e) {
            return null;
        }

        // Named colors
        try {
            return (Color) Color.class.getField(name.toUpperCase(Locale.ROOT)).get(null);
        } catch (ReflectiveOperationException | ClassCastException e1) {
            // Ignore
        }

        // RGB colors (like the CSS rgb function)
        Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
        Matcher m = c.matcher(name);
        if (m.matches()) {
            return Color.fromRGB(Integer.valueOf(m.group(1)),
                    Integer.valueOf(m.group(2)),
                    Integer.valueOf(m.group(3)));
        }

        // Give up
        return null;
    }

    private Map<Material, Color> colorsByMaterial = new EnumMap<>(Material.class);

    public ColorConfig(ConfigurationSection configurationSection, Logg log) {
        Set<String> materialNames = configurationSection.getKeys(false);
        materialNames.addAll(configurationSection.getDefaultSection().getKeys(false));
        for (String materialName : materialNames) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                log.warn("Invalid material in config: " + materialName);
                continue;
            }
            String colorCode = configurationSection.getString(materialName);
            Color color = parseColor(colorCode);
            if (color == null) {
                log.warn("Invalid color in config: " + configurationSection.get(materialName));
                continue;
            }
            colorsByMaterial.put(material, color);
        }
    }

    public Color getColor(Material material) {
        Color color = colorsByMaterial.get(material);
        if (color == null) {
            return Color.GRAY;
        }
        return color;
    }

    public void write(ConfigurationSection section) {
        for (Entry<Material, Color> entry : colorsByMaterial.entrySet()) {
            section.set(entry.getKey().toString(), encodeColor(entry.getValue()));
        }
    }

}
