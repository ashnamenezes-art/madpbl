package com.smartbus.app.models;

public class Driver {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String licenseNumber;
    private String assignedBusId;
    private String assignedRouteId;
    private boolean isOnDuty;
    private String profileImage;

    public Driver() {}

    public Driver(String uid, String name, String email, String phone, String licenseNumber,
                  String assignedBusId, String assignedRouteId, boolean isOnDuty, String profileImage) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.assignedBusId = assignedBusId;
        this.assignedRouteId = assignedRouteId;
        this.isOnDuty = isOnDuty;
        this.profileImage = profileImage;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getAssignedBusId() { return assignedBusId; }
    public void setAssignedBusId(String assignedBusId) { this.assignedBusId = assignedBusId; }

    public String getAssignedRouteId() { return assignedRouteId; }
    public void setAssignedRouteId(String assignedRouteId) { this.assignedRouteId = assignedRouteId; }

    public boolean isOnDuty() { return isOnDuty; }
    public void setOnDuty(boolean onDuty) { isOnDuty = onDuty; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}
