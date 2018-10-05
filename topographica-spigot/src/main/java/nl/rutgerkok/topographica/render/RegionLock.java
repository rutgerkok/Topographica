package nl.rutgerkok.topographica.render;

import java.util.Objects;

import nl.rutgerkok.topographica.util.Region;

/**
 * When this lock is open, the stored region is being rendered.
 *
 */
final class RegionLock implements AutoCloseable {

    private final Runnable onClose;
    final Region region;

    RegionLock(Runnable onClose, Region region) {
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        this.region = Objects.requireNonNull(region, "region");
    }

    @Override
    public void close() {
        onClose.run();
    }

    int getRegionX() {
        return region.getRegionX();
    }

    int getRegionZ() {
        return region.getRegionZ();
    }
}
