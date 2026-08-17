package kr.shkworld.shktown.core.navigation.model;

public record Position(String world, double x, double y, double z, float yaw, float pitch) {
    public Position(String world, double x, double y, double z) {
        this(world, x, y, z, 0.0f, 0.0f);
    }

    public double distance(Position other) {
        return Math.sqrt(distanceSquared(other));
    }

    public double distanceSquared(Position other) {
        if (other == null || !this.world.equals(other.world)) {
            return Double.POSITIVE_INFINITY;
        }
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
