/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customcrops.api.core.block;

import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.Tag;
import net.momirealms.customcrops.api.BukkitCustomCropsPlugin;
import net.momirealms.customcrops.api.action.ActionManager;
import net.momirealms.customcrops.api.context.Context;
import net.momirealms.customcrops.api.context.ContextKeys;
import net.momirealms.customcrops.api.core.*;
import net.momirealms.customcrops.api.core.mechanic.pot.PotConfig;
import net.momirealms.customcrops.api.core.mechanic.sprinkler.SprinklerConfig;
import net.momirealms.customcrops.api.core.world.CustomCropsBlockState;
import net.momirealms.customcrops.api.core.world.CustomCropsChunk;
import net.momirealms.customcrops.api.core.world.CustomCropsWorld;
import net.momirealms.customcrops.api.core.world.Pos3;
import net.momirealms.customcrops.api.core.wrapper.WrappedBreakEvent;
import net.momirealms.customcrops.api.core.wrapper.WrappedInteractEvent;
import net.momirealms.customcrops.api.core.wrapper.WrappedPlaceEvent;
import net.momirealms.customcrops.api.event.SprinklerBreakEvent;
import net.momirealms.customcrops.api.event.SprinklerFillEvent;
import net.momirealms.customcrops.api.event.SprinklerInteractEvent;
import net.momirealms.customcrops.api.event.SprinklerPlaceEvent;
import net.momirealms.customcrops.api.misc.NamedTextColor;
import net.momirealms.customcrops.api.misc.water.WateringMethod;
import net.momirealms.customcrops.api.requirement.RequirementManager;
import net.momirealms.customcrops.api.util.EventUtils;
import net.momirealms.customcrops.api.util.LocationUtils;
import net.momirealms.customcrops.api.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SprinklerBlock extends AbstractCustomCropsBlock {

    // Hibiscus start - Custom sprinkler effects
    /**
     * {@link Pos3} - The sprinkler position
     * <p>
     * {@link SprinklerConfig} - The config for this sprinkler
     */
    private final static Map<Pos3, SprinklerConfig> SPRINKLER_EFFECTS
            = new HashMap<>();
    /**
     * {@link Pos3} - The sprinkler position
     * <p>
     * {@link BukkitTask} - The bukkit task associated with this sprinkler effect
     */
    private final static Map<Pos3, BukkitTask> SPRINKLER_TASKS
            = new HashMap<>();

    // Hibiscus end
    public SprinklerBlock() {
        super(BuiltInBlockMechanics.SPRINKLER.key());
    }

    @Override
    public void randomTick(CustomCropsBlockState state, CustomCropsWorld<?> world, Pos3 location, boolean offlineTick) {
        // ignore scheduled tick
        if (world.setting().tickSprinklerMode() == 2) return;
        if (canTick(state, world.setting().tickSprinklerInterval())) {
            tickSprinkler(state, world, location, offlineTick);
        }
    }

    @Override
    public void scheduledTick(CustomCropsBlockState state, CustomCropsWorld<?> world, Pos3 location, boolean offlineTick) {
        // ignore random tick
        if (world.setting().tickSprinklerMode() == 1) return;
        if (canTick(state, world.setting().tickSprinklerInterval())) {
            tickSprinkler(state, world, location, offlineTick);
        }
    }
    // Hibiscus start - Custom sprinkler effects

    private void tickSprinklerEffect(@NotNull CustomCropsWorld<?> world, @NotNull Pos3 location) {
        if (SPRINKLER_TASKS.containsKey(location)) {
            return;
        }

        SprinklerConfig config = SPRINKLER_EFFECTS.get(location);

        Plugin plugin = BukkitCustomCropsPlugin.getInstance().getBootstrap();

        World bukkitWorld = world.bukkitWorld();
        Location bukkitLocation = location.toLocation(bukkitWorld);

        BukkitTask task = new BukkitRunnable() {
            private final int size = config.rawRange();

            private double currentRotation = 0.0;
            private double currentSize = 0.0;

            @Override
            public void run() {
                List<Player> targets = new ArrayList<>();
                double radiusSq = 10 * 10;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(bukkitWorld) && player.getLocation().distanceSquared(bukkitLocation) <= radiusSq) {
                        targets.add(player);
                    }
                }

                Location loc = bukkitLocation.clone();
                loc.add(0.5, 0, 0.5);

                double height = 0.5;
                for (double i = 0.0; i < currentSize; i += 0.1) {
                    if (i < size * 0.4) {
                        height += 0.05;
                    } else if (i >= size * 0.6) {
                        height -= 0.1;
                    } else {
                        height += 0.025;
                    }

                    for (double d = 1; d <= 8; d++) {
                        Location particleLoc = loc.clone();

                        double diff = ((d / 8) * 90 * 9) + currentRotation;
                        particleLoc.add(Math.cos(diff) * i, height, Math.sin(diff) * i);

                        for (Player player : targets) {
                            player.playSound(bukkitLocation, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER, SoundCategory.BLOCKS, 1, 1);
                            player.spawnParticle(Particle.WATER_DROP, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                currentSize += 0.1;
                currentRotation += 0.05;
                if (currentSize >= size) {
                    this.cancel();
                    SPRINKLER_TASKS.remove(location);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);

        SPRINKLER_TASKS.put(location, task);
    }
    // Hibiscus end

    @Override
    public void onBreak(WrappedBreakEvent event) {
        CustomCropsWorld<?> world = event.world();
        Location location = LocationUtils.toBlockLocation(event.location());
        Pos3 pos3 = Pos3.from(location);
        SprinklerConfig config = Registries.ITEM_TO_SPRINKLER.get(event.brokenID());
        if (config == null) {
            if (!BukkitCustomCropsPlugin.isReloading()) {
                world.removeBlockState(pos3);
            }
            return;
        }

        final Player player = event.playerBreaker();
        Context<Player> context = Context.player(player);
        context.updateLocation(location);
        CustomCropsBlockState state = fixOrGetState(world, pos3, config, event.brokenID());
        if (!RequirementManager.isSatisfied(context, config.breakRequirements())) {
            event.setCancelled(true);
            return;
        }

        SprinklerBreakEvent breakEvent = new SprinklerBreakEvent(event.entityBreaker(), event.blockBreaker(), location, state, config, event.reason());
        if (EventUtils.fireAndCheckCancel(breakEvent)) {
            event.setCancelled(true);
            return;
        }

        world.removeBlockState(pos3);
        ActionManager.trigger(context, config.breakActions());
    }

    @Override
    public void onPlace(WrappedPlaceEvent event) {
        SprinklerConfig config = Registries.ITEM_TO_SPRINKLER.get(event.placedID());
        if (config == null) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.player();
        Context<Player> context = Context.player(player);
        Location location = LocationUtils.toBlockLocation(event.location());
        context.updateLocation(location);
        if (!RequirementManager.isSatisfied(context, config.placeRequirements())) {
            event.setCancelled(true);
            return;
        }

        Pos3 pos3 = Pos3.from(event.location());
        CustomCropsWorld<?> world = event.world();
        if (world.setting().sprinklerPerChunk() >= 0) {
            if (world.testChunkLimitation(pos3, this.getClass(), world.setting().sprinklerPerChunk())) {
                event.setCancelled(true);
                ActionManager.trigger(context, config.reachLimitActions());
                return;
            }
        }

        CustomCropsBlockState state = createBlockState();
        id(state, config.id());
        water(state, config.threeDItemWithWater().equals(event.placedID()) ? 1 : 0);

        SprinklerPlaceEvent placeEvent = new SprinklerPlaceEvent(player, event.item(), event.hand(), event.location(), config, state);
        if (EventUtils.fireAndCheckCancel(placeEvent)) {
            event.setCancelled(true);
            return;
        }

        world.addBlockState(pos3, state);
        ActionManager.trigger(context, config.placeActions());
    }

    @Override
    public boolean isInstance(String id) {
        return Registries.ITEM_TO_SPRINKLER.containsKey(id);
    }

    @Override
    public void restore(Location location, CustomCropsBlockState state) {
        SprinklerConfig config = config(state);
        if (config == null) return;
        updateBlockAppearance(location, config, water(state) != 0);
    }

    @Override
    public void onInteract(WrappedInteractEvent event) {
        SprinklerConfig config = Registries.ITEM_TO_SPRINKLER.get(event.relatedID());
        if (config == null) {
            return;
        }

        final Player player = event.player();
        Location location = LocationUtils.toBlockLocation(event.location());
        Context<Player> context = Context.player(player);
        context.arg(ContextKeys.SLOT, event.hand());
        context.updateLocation(location);
        CustomCropsBlockState state = fixOrGetState(event.world(), Pos3.from(location), config, event.relatedID());
        if (!RequirementManager.isSatisfied(context, config.useRequirements())) {
            return;
        }

        int waterInSprinkler = water(state);
        String itemID = event.itemID();
        ItemStack itemInHand = event.itemInHand();

        context.arg(ContextKeys.STORAGE, config.storage());

        if (!config.infinite()) {
            for (WateringMethod method : config.wateringMethods()) {
                if (method.getUsed().equals(itemID) && method.getUsedAmount() <= itemInHand.getAmount()) {
                    if (method.checkRequirements(context)) {
                        if (waterInSprinkler >= config.storage()) {
                            context.arg(ContextKeys.CURRENT_WATER, waterInSprinkler);
                            context.arg(ContextKeys.WATER_BAR, Optional.ofNullable(config.waterBar()).map(it -> it.getWaterBar(waterInSprinkler, config.storage())).orElse(""));
                            ActionManager.trigger(context, config.fullWaterActions());
                            ActionManager.trigger(context, config.interactActions());
                        } else {
                            SprinklerFillEvent waterEvent = new SprinklerFillEvent(player, itemInHand, event.hand(), location, method, state, config);
                            if (EventUtils.fireAndCheckCancel(waterEvent))
                                return;
                            if (player.getGameMode() != GameMode.CREATIVE) {
                                itemInHand.setAmount(Math.max(0, itemInHand.getAmount() - method.getUsedAmount()));
                                if (method.getReturned() != null) {
                                    ItemStack returned = BukkitCustomCropsPlugin.getInstance().getItemManager().build(player, method.getReturned());
                                    if (returned != null) {
                                        PlayerUtils.giveItem(player, returned, method.getReturnedAmount());
                                    }
                                }
                            }
                            int currentWater = Math.min(config.storage(), waterInSprinkler + method.amountOfWater());
                            context.arg(ContextKeys.CURRENT_WATER, currentWater);
                            context.arg(ContextKeys.WATER_BAR, Optional.ofNullable(config.waterBar()).map(it -> it.getWaterBar(currentWater, config.storage())).orElse(""));
                            if (addWater(state, config, method.amountOfWater()) && !config.threeDItem().equals(config.threeDItemWithWater())) {
                                updateBlockAppearance(location, config, true);
                            }

                            method.triggerActions(context);
                            ActionManager.trigger(context, config.addWaterActions());
                            ActionManager.trigger(context, config.interactActions());
                        }
                    }
                    return;
                }
            }
        }

        context.arg(ContextKeys.WATER_BAR, Optional.ofNullable(config.waterBar()).map(it -> it.getWaterBar(waterInSprinkler, config.storage())).orElse(""));
        context.arg(ContextKeys.CURRENT_WATER, waterInSprinkler);

        SprinklerInteractEvent interactEvent = new SprinklerInteractEvent(player, itemInHand, location, config, state, event.hand());
        if (EventUtils.fireAndCheckCancel(interactEvent)) {
            return;
        }

        ActionManager.trigger(context, config.interactActions());
    }

    @Override
    public CustomCropsBlockState createBlockState(String itemID) {
        SprinklerConfig config = Registries.ITEM_TO_SPRINKLER.get(itemID);
        if (config == null) {
            return null;
        }
        CustomCropsBlockState state = createBlockState();
        id(state, config.id());
        water(state, itemID.equals(config.threeDItemWithWater()) ? 1 : 0);
        return state;
    }

    public CustomCropsBlockState fixOrGetState(CustomCropsWorld<?> world, Pos3 pos3, SprinklerConfig sprinklerConfig, String blockID) {
        Optional<CustomCropsBlockState> optionalPotState = world.getBlockState(pos3);
        if (optionalPotState.isPresent()) {
            CustomCropsBlockState potState = optionalPotState.get();
            if (potState.type() instanceof SprinklerBlock sprinklerBlock) {
                if (sprinklerBlock.id(potState).equals(sprinklerConfig.id())) {
                    return potState;
                }
            }
        }
        CustomCropsBlockState state = createBlockState();
        id(state, sprinklerConfig.id());
        water(state, blockID.equals(sprinklerConfig.threeDItemWithWater()) ? 1 : 0);
        world.addBlockState(pos3, state).ifPresent(previous -> {
            BukkitCustomCropsPlugin.getInstance().debug(() -> "Overwrite old data with " + state +
                    " at location[" + world.worldName() + "," + pos3 + "] which used to be " + previous
            );
        });
        return state;
    }

    public void tickSprinkler(CustomCropsBlockState state, CustomCropsWorld<?> world, Pos3 location, boolean offline) {
        SprinklerConfig config = config(state);
        if (config == null) {
            BukkitCustomCropsPlugin.getInstance().getPluginLogger().warn("Sprinkler data is removed at location[" + world.worldName() + "," + location + "] because the sprinkler config[" + id(state) + "] has been removed.");
            world.removeBlockState(location);
            return;
        }

        World bukkitWorld = world.bukkitWorld();
        Location bukkitLocation = location.toLocation(bukkitWorld);
        Context<CustomCropsBlockState> context = Context.block(state, bukkitLocation).arg(ContextKeys.OFFLINE, offline);
        if (!RequirementManager.isSatisfied(context, config.tickRequirements())) {
            return;
        }

        boolean updateState;
        if (!config.infinite()) {
            int water = water(state);
            if (water < config.sprinklingAmount()) {
                return;
            }
            updateState = water(state, config, water - config.sprinklingAmount());
        } else {
            updateState = false;
        }

        // place/remove entities on main thread
        BukkitCustomCropsPlugin.getInstance().getScheduler().sync().run(() -> {

            if (ConfigManager.doubleCheck()) {
                String modelID = BukkitCustomCropsPlugin.getInstance().getItemManager().id(bukkitLocation, config.existenceForm());
                if (modelID == null || !config.modelIDs().contains(modelID)) {
                    world.removeBlockState(location);
                    BukkitCustomCropsPlugin.getInstance().getPluginLogger().warn("Sprinkler[" + config.id() + "] is removed at Location[" + world.worldName() + "," + location + "] because the id of the block/furniture is " + modelID);
                    return;
                }
            }

            ActionManager.trigger(context, config.workActions());
            // Hibiscus start - Custom sprinkler effects

            SPRINKLER_EFFECTS.put(location, config);
            tickSprinklerEffect(world, location);

            // Hibiscus end
            if (updateState && !config.threeDItem().equals(config.threeDItemWithWater())) {
                updateBlockAppearance(bukkitLocation, config, false);
            }

            int[][] range = config.range();
            int length = range.length;
            Pos3[] pos3s = new Pos3[length * 2];
            for (int i = 0; i < length; i++) {
                int x = range[i][0];
                int z = range[i][1];
                pos3s[i] = location.add(x, 0, z);
                pos3s[i + length] = location.add(x, -1, z);
            }

            for (Pos3 pos3 : pos3s) {
                Optional<CustomCropsChunk> optionalChunk = world.getLoadedChunk(pos3.toChunkPos());
                if (optionalChunk.isPresent()) {
                    CustomCropsChunk chunk = optionalChunk.get();
                    Optional<CustomCropsBlockState> optionalState = chunk.getBlockState(pos3);
                    if (optionalState.isPresent()) {
                        CustomCropsBlockState anotherState = optionalState.get();
                        if (anotherState.type() instanceof PotBlock potBlock) {
                            PotConfig potConfig = potBlock.config(anotherState);
                            if (!potConfig.disablePluginMechanism()) {
                                if (config.potWhitelist().contains(potConfig.id())) {
                                    if (potBlock.addWater(anotherState, potConfig, config.wateringAmount())) {
                                        BukkitCustomCropsPlugin.getInstance().getScheduler().sync().run(
                                                () -> potBlock.updateBlockAppearance(
                                                        pos3.toLocation(world.bukkitWorld()),
                                                        potConfig,
                                                        true,
                                                        potBlock.fertilizers(anotherState)
                                                ),
                                                bukkitWorld,
                                                pos3.chunkX(), pos3.chunkZ()
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, bukkitLocation);
    }

    public boolean addWater(CustomCropsBlockState state, int water) {
        return water(state, water + water(state));
    }

    public boolean addWater(CustomCropsBlockState state, SprinklerConfig config, int water) {
        return water(state, config, water + water(state));
    }

    public int water(CustomCropsBlockState state) {
        Tag<?> tag = state.get("water");
        if (tag == null) {
            return 0;
        }
        return tag.getAsIntTag().map(IntTag::getValue).orElse(0);
    }

    public boolean water(CustomCropsBlockState state, int water) {
        return water(state, config(state), water);
    }

    public boolean water(CustomCropsBlockState state, SprinklerConfig config, int water) {
        if (water < 0) water = 0;
        int current = Math.min(water, config.storage());
        int previous = water(state);
        if (water == previous) return false;
        state.set("water", new IntTag("water", current));
        return previous == 0 ^ current == 0;
    }

    public SprinklerConfig config(CustomCropsBlockState state) {
        return Registries.SPRINKLER.get(id(state));
    }

    public void updateBlockAppearance(Location location, SprinklerConfig config, boolean hasWater) {
        FurnitureRotation rotation = BukkitCustomCropsPlugin.getInstance().getItemManager().remove(location, ExistenceForm.ANY);
        BukkitCustomCropsPlugin.getInstance().getItemManager().place(location, config.existenceForm(), hasWater ? config.threeDItemWithWater() : config.threeDItem(), rotation);
    }

    @Override
    public NamedTextColor insightColor() {
        return NamedTextColor.AQUA;
    }
}