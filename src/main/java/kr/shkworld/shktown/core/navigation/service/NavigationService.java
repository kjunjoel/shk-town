package kr.shkworld.shktown.core.navigation.service;

import kr.shkworld.shktown.core.navigation.model.Position;

import java.util.HashMap;
import java.util.Map;

public final class NavigationService {
    private double arrivalRadius;
    private final Map<String, Position> destinations = new HashMap<>();

    public void setArrivalRadius(double arrivalRadius) {
        this.arrivalRadius = Math.max(0.0, arrivalRadius);
    }

    public void registerDestination(String key, Position destination) {
        if (key != null && destination != null) destinations.put(key, destination);
    }

    public void clearDestinations() {
        destinations.clear();
    }

    public Position getDestination(String key) {
        return key == null ? null : destinations.get(key);
    }

    public Map<String, Position> getDestinations() {
        return Map.copyOf(destinations);
    }

    public boolean isArrived(Position current, Position destination) {
        return current != null && destination != null && current.distanceSquared(destination) <= arrivalRadius * arrivalRadius;
    }
}
