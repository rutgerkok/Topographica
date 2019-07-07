package nl.rutgerkok.topographica.config;

import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.StartupLog;

/**
 * Keeps track of how fast the plugin may run.
 *
 */
public final class TimingsConfig {
    private final double pauseSecondsAfterRenderPass;
    private final double pauseSecondsAfterChunkLoad;

    TimingsConfig(ConfigurationSection config, StartupLog log) {
        double pauseSecondsAfterRenderPass = config.getDouble("pause-seconds-after-render-pass");
        double pauseSecondsAfterChunkLoad = config.getDouble("pause-seconds-after-chunk-load");
        if (pauseSecondsAfterRenderPass < 0) {
            log.warn("pause-seconds-after-render-pass was negative, so it was changed to 0");
            pauseSecondsAfterRenderPass = 0;
        }
        if (pauseSecondsAfterChunkLoad < 0) {
            log.warn("pause-seconds-after-chunk-load was negative, so it was changed to 0");
            pauseSecondsAfterChunkLoad = 0;
        }
        this.pauseSecondsAfterRenderPass = pauseSecondsAfterRenderPass;
        this.pauseSecondsAfterChunkLoad = pauseSecondsAfterChunkLoad;
    }

    /**
     * Gets how many seconds the renderer should wait after it had to load a
     * chunk from disk.
     *
     * @return Amount of seconds.
     */
    public double getPauseSecondsAfterChunkLoad() {
        return pauseSecondsAfterChunkLoad;
    }

    /**
     * Gets how many seconds the renderer should wait after a full render pass.
     *
     * @return Amount of seconds.
     */
    public double getPauseSecondsAfterRenderPass() {
        return pauseSecondsAfterRenderPass;
    }

    void write(ConfigurationSection to) {
        to.set("pause-seconds-after-render-pass", this.pauseSecondsAfterRenderPass);
        to.set("pause-seconds-after-chunk-load", this.pauseSecondsAfterChunkLoad);
    }
}
