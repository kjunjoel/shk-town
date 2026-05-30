package kr.shkworld.shktown.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

public class WorldGuardHook {
    private WorldGuard worldGuardInstance;

    public boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            return false;
        }

        this.worldGuardInstance = WorldGuard.getInstance();
        return this.worldGuardInstance != null;
    }

    public Optional<RegionContainer> getContainer() {
        if (worldGuardInstance == null) {
            return Optional.empty();
        }

        return Optional.of(worldGuardInstance.getPlatform().getRegionContainer());
    }

    public Optional<String> getCurrentRegionId(Player player) {
        if (worldGuardInstance == null) {
            return Optional.empty();
        }

        RegionQuery query = worldGuardInstance.getPlatform().getRegionContainer().createQuery();
        Location loc = BukkitAdapter.adapt(player.getLocation());
        ApplicableRegionSet set = query.getApplicableRegions(loc);

        ProtectedRegion highestPriorityRegion = null;

        for (ProtectedRegion region : set) {
            if (highestPriorityRegion == null) {
                highestPriorityRegion = region;
                continue;
            }

            if (region.getPriority() > highestPriorityRegion.getPriority()) {
                highestPriorityRegion = region;
            }
        }

        return highestPriorityRegion != null ? Optional.of(highestPriorityRegion.getId()) : Optional.empty();
    }
}
