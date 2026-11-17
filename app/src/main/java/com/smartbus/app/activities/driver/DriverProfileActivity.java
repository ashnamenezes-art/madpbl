package com.smartbus.app.activities.driver;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Driver;
import com.smartbus.app.models.Route;
import com.smartbus.app.utils.SharedPrefManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriverProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private CircleImageView ivAvatar;
    private TextView tvDriverName;
    private TextView tvLicenseNumber;
    private Chip chipDutyStatus;
    private TextView tvPhone;
    private TextView tvEmail;
    private TextView tvAssignedBus;
    private TextView tvAssignedRoute;
    private MaterialButton btnEditProfile;

    private FirebaseHelper firebaseHelper;
    private SharedPrefManager prefManager;
    private String driverId;
    private Driver currentDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);

        firebaseHelper = FirebaseHelper.getInstance();
        prefManager = SharedPrefManager.getInstance(this);
        driverId = prefManager.getUserId();
        if (driverId == null) {
            driverId = firebaseHelper.getCurrentUserId();
        }

        initViews();
        setupToolbar();
        loadProfileData();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvLicenseNumber = findViewById(R.id.tvLicenseNumber);
        chipDutyStatus = findViewById(R.id.chipDutyStatus);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvAssignedBus = findViewById(R.id.tvAssignedBus);
        tvAssignedRoute = findViewById(R.id.tvAssignedRoute);
        btnEditProfile = findViewById(R.id.btnEditProfile);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadProfileData() {
        if (driverId == null) {
            Toast.makeText(this, "Driver not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseHelper.getDriversRef().child(driverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentDriver = snapshot.getValue(Driver.class);
                if (currentDriver != null) {
                    populateDriverUI(currentDriver);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateDriverUI(Driver driver) {
        tvDriverName.setText(driver.getName() != null ? driver.getName() : "Driver");
        tvLicenseNumber.setText("License: " + (driver.getLicenseNumber() != null ? driver.getLicenseNumber() : "N/A"));
        tvPhone.setText(driver.getPhone() != null && !driver.getPhone().isEmpty() ? driver.getPhone() : "Not provided");
        tvEmail.setText(driver.getEmail() != null && !driver.getEmail().isEmpty() ? driver.getEmail() : "Not provided");

        if (driver.isOnDuty()) {
            chipDutyStatus.setText("On Duty");
            chipDutyStatus.setTextColor(ContextCompat.getColor(this, R.color.driver_color));
        } else {
            chipDutyStatus.setText("Off Duty");
            chipDutyStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        if (driver.getProfileImage() != null && !driver.getProfileImage().isEmpty()) {
            Glide.with(this)
                    .load(driver.getProfileImage())
                    .placeholder(R.drawable.ic_driver)
                    .into(ivAvatar);
        }

        // Assigned Bus
        if (driver.getAssignedBusId() != null && !driver.getAssignedBusId().isEmpty()) {
            firebaseHelper.getBusesRef().child(driver.getAssignedBusId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Bus bus = snapshot.getValue(Bus.class);
                    if (bus != null) {
                        tvAssignedBus.setText("Bus #" + bus.getBusNumber() + " (" + bus.getBusType() + ")");
                    } else {
                        tvAssignedBus.setText("Bus ID: " + driver.getAssignedBusId());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvAssignedBus.setText("Bus ID: " + driver.getAssignedBusId());
                }
            });
        } else {
            tvAssignedBus.setText("No Bus Assigned");
        }

        // Assigned Route
        if (driver.getAssignedRouteId() != null && !driver.getAssignedRouteId().isEmpty()) {
            firebaseHelper.getRoutesRef().child(driver.getAssignedRouteId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Route route = snapshot.getValue(Route.class);
                    if (route != null) {
                        tvAssignedRoute.setText(route.getRouteName() + " (" + route.getSourceStop() + " ➔ " + route.getDestinationStop() + ")");
                    } else {
                        tvAssignedRoute.setText("Route ID: " + driver.getAssignedRouteId());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvAssignedRoute.setText("Route ID: " + driver.getAssignedRouteId());
                }
            });
        } else {
            tvAssignedRoute.setText("No Route Assigned");
        }
    }

    private void showEditProfileDialog() {
        if (currentDriver == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_driver_profile, null);
        TextInputEditText etEditName = dialogView.findViewById(R.id.etEditName);
        TextInputEditText etEditPhone = dialogView.findViewById(R.id.etEditPhone);
        TextInputEditText etEditLicense = dialogView.findViewById(R.id.etEditLicense);

        etEditName.setText(currentDriver.getName());
        etEditPhone.setText(currentDriver.getPhone());
        etEditLicense.setText(currentDriver.getLicenseNumber());
        etEditLicense.setEnabled(false); // License cannot be modified by driver

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", null) // override below for validation
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton btnSave = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSave.setOnClickListener(view -> {
                String newName = etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
                String newPhone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

                if (TextUtils.isEmpty(newName)) {
                    etEditName.setError("Name cannot be empty");
                    return;
                }

                saveProfileChanges(newName, newPhone);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void saveProfileChanges(String newName, String newPhone) {
        if (driverId == null) return;

        firebaseHelper.getDriversRef().child(driverId).child("name").setValue(newName);
        firebaseHelper.getDriversRef().child(driverId).child("phone").setValue(newPhone);

        // Also update users node
        firebaseHelper.getUsersRef().child(driverId).child("name").setValue(newName);
        firebaseHelper.getUsersRef().child(driverId).child("phone").setValue(newPhone);

        if (currentDriver != null) {
            currentDriver.setName(newName);
            currentDriver.setPhone(newPhone);
            populateDriverUI(currentDriver);
        }

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}
