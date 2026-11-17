package com.smartbus.app.models;

public class Complaint {
    private String complaintId;
    private String passengerId;
    private String busId;
    private String subject;
    private String description;
    private long timestamp;
    private String status; // "pending" | "in_review" | "resolved"
    private String adminReply;

    public Complaint() {}

    public Complaint(String complaintId, String passengerId, String busId, String subject,
                     String description, long timestamp, String status, String adminReply) {
        this.complaintId = complaintId;
        this.passengerId = passengerId;
        this.busId = busId;
        this.subject = subject;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
        this.adminReply = adminReply;
    }

    public String getComplaintId() { return complaintId; }
    public void setComplaintId(String complaintId) { this.complaintId = complaintId; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
}
