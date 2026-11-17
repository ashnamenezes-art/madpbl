package com.smartbus.app.models;

public class Stop {
    private String stopId;
    private String stopName;
    private String routeId;
    private int stopOrder;
    private double latitude;
    private double longitude;
    private String address;

    public Stop() {}

    public Stop(String stopId, String stopName, String routeId, int stopOrder,
                double latitude, double longitude, String address) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.routeId = routeId;
        this.stopOrder = stopOrder;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public String getStopId() { return stopId; }
    public void setStopId(String stopId) { this.stopId = stopId; }

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public int getStopOrder() { return stopOrder; }
    public void setStopOrder(int stopOrder) { this.stopOrder = stopOrder; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
