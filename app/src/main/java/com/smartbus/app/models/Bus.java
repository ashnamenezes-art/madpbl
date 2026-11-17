package com.smartbus.app.models;

public class Bus {
    private String busId;
    private String busNumber;
    private String busName;
    private String routeId;
    private String driverId;
    private int totalSeats;
    private String busType;
    private boolean isActive;
    private double currentLatitude;
    private double currentLongitude;
    private long lastUpdated;

    public Bus() {}

    public Bus(String busId, String busNumber, String busName, String routeId, String driverId,
               int totalSeats, String busType, boolean isActive,
               double currentLatitude, double currentLongitude, long lastUpdated) {
        this.busId = busId;
        this.busNumber = busNumber;
        this.busName = busName;
        this.routeId = routeId;
        this.driverId = driverId;
        this.totalSeats = totalSeats;
        this.busType = busType;
        this.isActive = isActive;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.lastUpdated = lastUpdated;
    }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getBusName() { return busName; }
    public void setBusName(String busName) { this.busName = busName; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(double currentLatitude) { this.currentLatitude = currentLatitude; }

    public double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(double currentLongitude) { this.currentLongitude = currentLongitude; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
