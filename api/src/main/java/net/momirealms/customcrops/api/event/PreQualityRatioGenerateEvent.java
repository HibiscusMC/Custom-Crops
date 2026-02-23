package net.momirealms.customcrops.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An event that is triggered before the quality ratios on a quality crop are finally determined
 */
public class PreQualityRatioGenerateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String cropId;
    private final Player player;
    private final List<double[]> qualityRatios;

    public PreQualityRatioGenerateEvent(String cropId, Player player, List<double[]> qualityRatios) {
        this.cropId = cropId;
        this.player = player;
        this.qualityRatios = qualityRatios;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return the static handler list.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Gets the list of handlers for this event instance.
     *
     * @return the handler list.
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public String cropId() {
        return cropId;
    }

    public Player player() {
        return player;
    }

    public void addQualityRatios(double[] ratio) {
        qualityRatios.add(ratio);
    }
}