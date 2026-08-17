package kr.shkworld.shktown.ui.apps.navigation;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.navigation.model.Position;
import kr.shkworld.shktown.util.LocationUtil;
import kr.shkworld.shktown.util.MessageUtil;
import kr.shkworld.shktown.util.TextUtil;
import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.adapter.LocationWrapper;
import kr.toxicity.hud.api.adapter.WorldWrapper;
import kr.toxicity.hud.api.player.HudPlayer;
import kr.toxicity.hud.api.player.PointedLocation;
import kr.toxicity.hud.api.player.PointedLocationSource;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NavigationManager {
    private final SHKTown plugin;

    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, NavigationSession> activeSessions = new HashMap<>();
    private final Map<String, ItemStack> destinationItems = new HashMap<>();
    private final Map<String, String> destinationNames = new HashMap<>();
    private String title = "";
    private String pointerId = "";
    private int updateIntervalTicks;
    private boolean useGlobalPrefix;
    private String started = "", stopped = "", arrived = "", differentWorld = "";

    public NavigationManager(SHKTown plugin) {
        this.plugin = plugin;
    }

    public void configure(String title, String pointerId, int updateIntervalTicks, boolean useGlobalPrefix,
                          String started, String stopped, String arrived, String differentWorld) {
        this.title = title; this.pointerId = pointerId; this.updateIntervalTicks = updateIntervalTicks;
        this.useGlobalPrefix = useGlobalPrefix; this.started = started; this.stopped = stopped;
        this.arrived = arrived; this.differentWorld = differentWorld;
    }

    public void clearDestinationItems() {
        destinationItems.clear(); destinationNames.clear();
    }

    public void registerDestinationItem(String key, String name, ItemStack item) {
        destinationNames.put(key, name); destinationItems.put(key, item);
    }

    public ItemStack getDestinationItem(String key) {
        return destinationItems.get(key);
    }

    public String getDestinationName(String key) {
        return destinationNames.getOrDefault(key, key);
    }

    public String getTitle() {
        return title;
    }

    public String getDifferentWorld() {
        return differentWorld;
    }

    public void startNavigation(Player player, String destinationName, Location destination) {
        UUID uuid = player.getUniqueId();
        stopNavigationInternal(player);

        if (!player.getWorld().equals(destination.getWorld())) {
            MessageUtil.send(player, differentWorld, useGlobalPrefix);
            return;
        }

        activeSessions.put(uuid, new NavigationSession(destinationName, destination));

        try {
            HudPlayer hudPlayer = BetterHudAPI.inst().getPlayerManager().getHudPlayer(uuid);
            if (hudPlayer != null) {
                PointedLocation pointedLocation = getPointedLocation(destination, pointerId);

                hudPlayer.pointers().add(pointedLocation);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("BetterHUD Pointer registration failed: " + e.getMessage());
        }

        String startMessage = started.replace("{destination}", destinationName);
        MessageUtil.send(player, startMessage, useGlobalPrefix);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopNavigation(player, false);
                    return;
                }

                Location currentLocation = player.getLocation();
                if (!currentLocation.getWorld().equals(destination.getWorld())) {
                    stopNavigation(player, false);
                    return;
                }

                double distance = currentLocation.distance(destination);

                if (plugin.getNavigationService().isArrived(LocationUtil.toPosition(currentLocation), LocationUtil.toPosition(destination))) {
                    stopNavigation(player, true);
                    return;
                }

                Position fromPos = LocationUtil.toPosition(currentLocation);
                Position toPos = LocationUtil.toPosition(destination);
                NavigationDirection dir = NavigationDirection.calculate(fromPos, toPos, 10.0);

                int distanceInt = (int) Math.round(distance);
                String formattedActionBar = String.format("&b%s &f| %dm &f| &a%s", destinationName, distanceInt, dir.getText());
                player.sendActionBar(TextUtil.parse(formattedActionBar));

                spawnDestinationParticles(player, destination);
            }
        }.runTaskTimer(plugin, 0L, Math.max(1, updateIntervalTicks));

        activeTasks.put(uuid, task);
    }

    private static PointedLocation getPointedLocation(Location destination, String pointerId) {
        LocationWrapper locationWrapper = new LocationWrapper(
                new WorldWrapper(destination.getWorld().getName()),
                destination.getX(),
                destination.getY(),
                destination.getZ(),
                0.0f, 0.0f
        );

        return new PointedLocation(
                PointedLocationSource.INTERNAL,
                pointerId,
                pointerId,
                locationWrapper
        );
    }

    public void stopNavigation(Player player, boolean isArrived) {
        UUID uuid = player.getUniqueId();

        NavigationSession session = activeSessions.get(uuid);
        boolean wasNavigation = stopNavigationInternal(player);

        if (!wasNavigation || session == null) {
            return;
        }

        if (isArrived) {
            String arrivedMessage = arrived.replace("{destination}", session.name());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            MessageUtil.send(player, arrivedMessage, useGlobalPrefix);
            MessageUtil.sendTitle(player, "§a✔ 목적지 도착!", "§f" + session.name() + "에 도착했습니다.");
        } else {
            MessageUtil.send(player, stopped, useGlobalPrefix);
        }
    }

    private boolean stopNavigationInternal(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = activeTasks.remove(uuid);
        boolean hadTask = (task != null);

        if (hadTask) {
            task.cancel();
        }

        activeSessions.remove(uuid);

        try {
            HudPlayer hudPlayer = BetterHudAPI.inst().getPlayerManager().getHudPlayer(uuid);
            if (hudPlayer != null) {
                hudPlayer.pointers().removeIf(pt -> pointerId.equalsIgnoreCase(pt.name()));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("BetterHUD Pointer removal failed for " + player.getName() + ": " + e.getMessage());
        }

        if (player.isOnline()) {
            player.sendActionBar(TextUtil.parse(""));
        }

        return hadTask;
    }

    private void spawnDestinationParticles(Player player, Location destination) {
        if (destination.getWorld() == null) return;

        for (double yOffset = 0; yOffset <= 10; yOffset += 0.5) {
            Location particleLocation = destination.clone().add(0, yOffset, 0);

            player.spawnParticle(
                    Particle.FIREWORK,
                    particleLocation,
                    1,
                    0.1, 0.1, 0.1,
                    0.01
            );

            if (yOffset == 0) {
                player.spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        particleLocation,
                        5,
                        0.3, 0.1, 0.3,
                        0.0
                );
            }
        }
    }

    public boolean isNavigating(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    public String getDestinationName(Player player) {
        if (player == null) return null;
        NavigationSession session = activeSessions.get(player.getUniqueId());
        return session != null ? session.name() : null;
    }

    public void openScreen(Player player) {
        NavigationScreen navigationScreen = new NavigationScreen(plugin, player);
        player.openInventory(navigationScreen.getInventory());
    }

    private record NavigationSession(String name, Location location) {}
}
