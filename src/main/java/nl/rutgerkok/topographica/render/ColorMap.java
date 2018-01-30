package nl.rutgerkok.topographica.render;

import org.bukkit.Color;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;

/**
 * A class that maps materials to colors.
 *
 */
final class ColorMap {

    public Color getColor(MaterialData blockData) {
        switch (blockData.getItemType()) {
            case WATER:
            case STATIONARY_WATER:
                return Color.BLUE;
            case LAVA:
            case STATIONARY_LAVA:
                return Color.ORANGE;
            case GRASS:
                return Color.GREEN;
            case DIRT:
                return Color.MAROON;
            case STONE:
                return Color.GRAY;
            case AIR:
                return Color.BLACK;
            default:
                if (blockData instanceof Colorable) {
                    return ((Colorable) blockData).getColor().getColor();
                }
                return Color.GRAY;
        }
    }

}
