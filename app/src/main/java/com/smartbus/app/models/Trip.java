package com.smartbus.app.models;

public class Trip {
    private String tripId;
    private String busId;
    private String driverId;
    private String routeId;
    private long startTime;
    private long endTime;
    private String status; // "active" | "completed" | "cancelled"
    private double startLatitude;
    private double startLongitude;

    public Trip() {}

    public Trip(String tripId, String busId, String driverId, String routeId,
                long startTime, long endTime, String status,
                double startLatitude, double startLongitude) {
        this.tripId = tripId;
        this.busId = busId;
        this.driverId = driverId;
        this.routeId = routeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getStartLatitude() { return startLatitude; }
    public void setStartLatitude(double startLatitude) { this.startLatitude = startLatitude; }

    public double getStartLongitude() { return startLongitude; }
    public void setStartLongitude(double startLongitude) { this.startLongitude = startLongitude; }
}
