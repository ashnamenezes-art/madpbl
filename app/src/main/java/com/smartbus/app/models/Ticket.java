package com.smartbus.app.models;

public class Ticket {
    private String ticketId;
    private String passengerId;
    private String busId;
    private String routeId;
    private String fromStop;
    private String toStop;
    private double fareAmount;
    private long purchaseTime;
    private String status;
    private String qrCode;

    public Ticket() {}

    public Ticket(String ticketId, String passengerId, String busId, String routeId,
                  String fromStop, String toStop, double fareAmount,
                  long purchaseTime, String status, String qrCode) {
        this.ticketId = ticketId;
        this.passengerId = passengerId;
        this.busId = busId;
        this.routeId = routeId;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.fareAmount = fareAmount;
        this.purchaseTime = purchaseTime;
        this.status = status;
        this.qrCode = qrCode;
    }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getFromStop() { return fromStop; }
    public void setFromStop(String fromStop) { this.fromStop = fromStop; }

    public String getToStop() { return toStop; }
    public void setToStop(String toStop) { this.toStop = toStop; }

    public double getFareAmount() { return fareAmount; }
    public void setFareAmount(double fareAmount) { this.fareAmount = fareAmount; }

    public long getPurchaseTime() { return purchaseTime; }
    public void setPurchaseTime(long purchaseTime) { this.purchaseTime = purchaseTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
}
