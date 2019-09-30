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
    private final boolean noPauseAfterChunkLoadWhenNobodyIsOnline;

    TimingsConfig(ConfigurationSection config, StartupLog log) {
        double pauseSecondsAfterRenderPass = config.getDouble("pause-seconds-after-render-pass");
        double pauseSecondsAfterChunkLoad = config.getDouble("pause-seconds-after-chunk-load");
        noPauseAfterChunkLoadWhenNobodyIsOnline = config.getBoolean("no-pause-after-chunk-load-when-nobody-is-online");
        if (pauseSecondsAfterRenderPass < 0.5) {
            log.warn("pause-seconds-after-render-pass was too small, so it was changed to 0.5");
            pauseSecondsAfterRenderPass = 0.5;
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
     * @param playersOnline
     *            The number of players online in the server.
     *
     * @return Amount of seconds.
     */
    public double getPauseSecondsAfterChunkLoad(int playersOnline) {
        if (playersOnline == 0 && this.noPauseAfterChunkLoadWhenNobodyIsOnline) {
            return 0;
        }
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
        to.set("no-pause-after-chunk-load-when-nobody-is-online", this.noPauseAfterChunkLoadWhenNobodyIsOnline);
    }
}
