package com.smartbus.app.models;

public class Route {
    private String routeId;
    private String routeName;
    private String sourceStop;
    private String destinationStop;
    private int totalStops;
    private double distanceKm;
    private int estimatedDuration; // in minutes
    private double fareAmount;
    private boolean isActive;

    public Route() {}

    public Route(String routeId, String routeName, String sourceStop, String destinationStop,
                 int totalStops, double distanceKm, int estimatedDuration,
                 double fareAmount, boolean isActive) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.sourceStop = sourceStop;
        this.destinationStop = destinationStop;
        this.totalStops = totalStops;
        this.distanceKm = distanceKm;
        this.estimatedDuration = estimatedDuration;
        this.fareAmount = fareAmount;
        this.isActive = isActive;
    }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getSourceStop() { return sourceStop; }
    public void setSourceStop(String sourceStop) { this.sourceStop = sourceStop; }

    public String getDestinationStop() { return destinationStop; }
    public void setDestinationStop(String destinationStop) { this.destinationStop = destinationStop; }

    public int getTotalStops() { return totalStops; }
    public void setTotalStops(int totalStops) { this.totalStops = totalStops; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(int estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public double getFareAmount() { return fareAmount; }
    public void setFareAmount(double fareAmount) { this.fareAmount = fareAmount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
