package com.smartbus.app.models;

public class Feedback {
    private String feedbackId;
    private String passengerId;
    private String busId;
    private String driverId;
    private float rating;
    private String comment;
    private long timestamp;
    private boolean isResolved;

    public Feedback() {}

    public Feedback(String feedbackId, String passengerId, String busId, String driverId,
                    float rating, String comment, long timestamp, boolean isResolved) {
        this.feedbackId = feedbackId;
        this.passengerId = passengerId;
        this.busId = busId;
        this.driverId = driverId;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
        this.isResolved = isResolved;
    }

    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isResolved() { return isResolved; }
    public void setResolved(boolean resolved) { isResolved = resolved; }
}
