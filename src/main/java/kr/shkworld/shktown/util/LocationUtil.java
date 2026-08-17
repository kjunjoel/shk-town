package kr.shkworld.shktown.util;

import kr.shkworld.shktown.core.navigation.model.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {
    private LocationUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    public static Position toPosition(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        return new Position(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static Location toLocation(Position position) {
        if (position == null || position.world() == null) {
            return null;
        }

        World world = Bukkit.getWorld(position.world());
        if (world == null) {
            return null;
        }

        return new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
    }
}
