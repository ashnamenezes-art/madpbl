package com.smartbus.app.models;

public class LiveLocation {
    private String busId;
    private String driverId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String tripStatus;
    private float speed;
    private float heading;

    public LiveLocation() {}

    public LiveLocation(String busId, String driverId, double latitude, double longitude,
                        long timestamp, String tripStatus, float speed, float heading) {
        this.busId = busId;
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.tripStatus = tripStatus;
        this.speed = speed;
        this.heading = heading;
    }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getTripStatus() { return tripStatus; }
    public void setTripStatus(String tripStatus) { this.tripStatus = tripStatus; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public float getHeading() { return heading; }
    public void setHeading(float heading) { this.heading = heading; }
}
