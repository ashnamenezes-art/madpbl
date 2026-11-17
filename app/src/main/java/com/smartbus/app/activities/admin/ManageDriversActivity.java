package com.smartbus.app.activities.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.AdminDriverAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Driver;
import com.smartbus.app.models.User;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class ManageDriversActivity extends AppCompatActivity {

    private ChipGroup chipGroupFilter;
    private RecyclerView rvDrivers;
    private ProgressBar progressBar;
    private TextView tvNoDrivers;
    private FloatingActionButton fabAddDriver;

    private AdminDriverAdapter driverAdapter;
    private final List<Driver> allDrivers = new ArrayList<>();
    private final List<Driver> filteredDrivers = new ArrayList<>();
    private final List<Bus> availableBuses = new ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private ValueEventListener driversListener;
    private ValueEventListener busesListener;

    private int currentFilterMode = 0; // 0 = All, 1 = On Duty, 2 = Off Duty

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_drivers);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilterChips();
        loadData();
    }

    private void initViews() {
        chipGroupFilter = findViewById(R.id.chip_group_filter);
        rvDrivers = findViewById(R.id.rv_drivers);
        progressBar = findViewById(R.id.progress_bar);
        tvNoDrivers = findViewById(R.id.tv_no_drivers);
        fabAddDriver = findViewById(R.id.fab_add_driver);

        fabAddDriver.setOnClickListener(v -> showAddDriverDialog());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Drivers");
        }
    }

    private void setupRecyclerView() {
        driverAdapter = new AdminDriverAdapter(this, filteredDrivers, new AdminDriverAdapter.OnAdminDriverActionListener() {
            @Override
            public void onDriverClick(Driver driver) {
                showEditDriverDialog(driver);
            }

            @Override
            public void onEditClick(Driver driver) {
                showEditDriverDialog(driver);
            }

            @Override
            public void onDeleteClick(Driver driver) {
                confirmDeleteDriver(driver);
            }

            @Override
            public void onAssignBusClick(Driver driver) {
                showAssignBusDialog(driver);
            }
        });

        rvDrivers.setLayoutManager(new LinearLayoutManager(this));
        rvDrivers.setAdapter(driverAdapter);
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_on_duty) {
                currentFilterMode = 1;
            } else if (checkedId == R.id.chip_off_duty) {
                currentFilterMode = 2;
            } else {
                currentFilterMode = 0;
            }
            applyFilter();
        });
    }

    private void applyFilter() {
        filteredDrivers.clear();
        for (Driver driver : allDrivers) {
            if (currentFilterMode == 1 && !driver.isOnDuty()) continue;
            if (currentFilterMode == 2 && driver.isOnDuty()) continue;
            filteredDrivers.add(driver);
        }

        driverAdapter.notifyDataSetChanged();
        if (filteredDrivers.isEmpty()) {
            tvNoDrivers.setVisibility(View.VISIBLE);
            rvDrivers.setVisibility(View.GONE);
        } else {
            tvNoDrivers.setVisibility(View.GONE);
            rvDrivers.setVisibility(View.VISIBLE);
        }
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        // Load Buses for assigning
        busesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableBuses.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null) {
                        if (bus.getBusId() == null) bus.setBusId(child.getKey());
                        availableBuses.add(bus);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getBusesRef().addValueEventListener(busesListener);

        // Load Drivers
        driversListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                allDrivers.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Driver driver = child.getValue(Driver.class);
                    if (driver != null) {
                        if (driver.getUid() == null) driver.setUid(child.getKey());
                        allDrivers.add(driver);
                    }
                }
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageDriversActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        firebaseHelper.getDriversRef().addValueEventListener(driversListener);
    }

    private void showAddDriverDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_driver, null);

        EditText etName = dialogView.findViewById(R.id.et_dialog_driver_name);
        EditText etEmail = dialogView.findViewById(R.id.et_dialog_driver_email);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_driver_phone);
        EditText etLicense = dialogView.findViewById(R.id.et_dialog_driver_license);
        Spinner spBus = dialogView.findViewById(R.id.sp_dialog_assigned_bus);

        List<String> busDisplayList = new ArrayList<>();
        busDisplayList.add("-- None --");
        for (Bus b : availableBuses) {
            String label = (b.getBusNumber() != null ? b.getBusNumber() : "Bus") + " (" + b.getBusName() + ")";
            busDisplayList.add(label);
        }
        ArrayAdapter<String> busSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, busDisplayList);
        spBus.setAdapter(busSpinnerAdapter);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Register New Driver")
                .setView(dialogView)
                .setPositiveButton("Register", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String license = etLicense.getText() != null ? etLicense.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Driver name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String assignedBusId = "";
                    int busPos = spBus.getSelectedItemPosition();
                    if (busPos > 0 && (busPos - 1) < availableBuses.size()) {
                        assignedBusId = availableBuses.get(busPos - 1).getBusId();
                    }

                    DatabaseReference driversRef = firebaseHelper.getDriversRef();
                    String uid = driversRef.push().getKey();

                    Driver newDriver = new Driver(
                            uid,
                            name,
                            email,
                            phone,
                            license,
                            assignedBusId,
                            "", // assignedRouteId
                            false, // isOnDuty
                            "" // profileImage
                    );

                    if (uid != null) {
                        driversRef.child(uid).setValue(newDriver);

                        // Also create in users/ node with role driver
                        User user = new User(uid, name, email, phone, Constants.ROLE_DRIVER, "");
                        firebaseHelper.getUsersRef().child(uid).setValue(user);

                        if (!TextUtils.isEmpty(assignedBusId)) {
                            firebaseHelper.getBusesRef().child(assignedBusId).child("driverId").setValue(uid);
                        }

                        Toast.makeText(this, "Driver added successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDriverDialog(final Driver driver) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_driver, null);

        EditText etName = dialogView.findViewById(R.id.et_dialog_driver_name);
        EditText etEmail = dialogView.findViewById(R.id.et_dialog_driver_email);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_driver_phone);
        EditText etLicense = dialogView.findViewById(R.id.et_dialog_driver_license);
        Spinner spBus = dialogView.findViewById(R.id.sp_dialog_assigned_bus);

        etName.setText(driver.getName());
        etEmail.setText(driver.getEmail());
        etEmail.setEnabled(false); // Email read-only after creation
        etPhone.setText(driver.getPhone());
        etLicense.setText(driver.getLicenseNumber());

        List<String> busDisplayList = new ArrayList<>();
        busDisplayList.add("-- None --");
        int selectedBusIndex = 0;
        for (int i = 0; i < availableBuses.size(); i++) {
            Bus b = availableBuses.get(i);
            String label = (b.getBusNumber() != null ? b.getBusNumber() : "Bus") + " (" + b.getBusName() + ")";
            busDisplayList.add(label);
            if (driver.getAssignedBusId() != null && driver.getAssignedBusId().equals(b.getBusId())) {
                selectedBusIndex = i + 1;
            }
        }
        ArrayAdapter<String> busSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, busDisplayList);
        spBus.setAdapter(busSpinnerAdapter);
        spBus.setSelection(selectedBusIndex);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Driver Details")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                    String license = etLicense.getText() != null ? etLicense.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Driver name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String assignedBusId = "";
                    int busPos = spBus.getSelectedItemPosition();
                    if (busPos > 0 && (busPos - 1) < availableBuses.size()) {
                        assignedBusId = availableBuses.get(busPos - 1).getBusId();
                    }

                    driver.setName(name);
                    driver.setPhone(phone);
                    driver.setLicenseNumber(license);
                    driver.setAssignedBusId(assignedBusId);

                    if (driver.getUid() != null) {
                        firebaseHelper.getDriversRef().child(driver.getUid()).setValue(driver);
                        firebaseHelper.getUsersRef().child(driver.getUid()).child("name").setValue(name);
                        firebaseHelper.getUsersRef().child(driver.getUid()).child("phone").setValue(phone);

                        if (!TextUtils.isEmpty(assignedBusId)) {
                            firebaseHelper.getBusesRef().child(assignedBusId).child("driverId").setValue(driver.getUid());
                        }

                        Toast.makeText(this, "Driver details updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAssignBusDialog(final Driver driver) {
        if (availableBuses.isEmpty()) {
            Toast.makeText(this, "No buses available to assign", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> busNames = new ArrayList<>();
        busNames.add("Unassign Bus");
        int currentSelection = 0;

        for (int i = 0; i < availableBuses.size(); i++) {
            Bus b = availableBuses.get(i);
            String label = (b.getBusNumber() != null ? b.getBusNumber() : "Bus") + " - " + (b.getBusName() != null ? b.getBusName() : "");
            busNames.add(label);
            if (driver.getAssignedBusId() != null && driver.getAssignedBusId().equals(b.getBusId())) {
                currentSelection = i + 1;
            }
        }

        final int[] selected = {currentSelection};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Assign Bus to " + (driver.getName() != null ? driver.getName() : "Driver"))
                .setSingleChoiceItems(busNames.toArray(new String[0]), currentSelection, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    if (driver.getUid() == null) return;

                    if (selected[0] == 0) {
                        // Unassign
                        String oldBusId = driver.getAssignedBusId();
                        driver.setAssignedBusId("");
                        firebaseHelper.getDriversRef().child(driver.getUid()).child("assignedBusId").setValue("");
                        if (!TextUtils.isEmpty(oldBusId)) {
                            firebaseHelper.getBusesRef().child(oldBusId).child("driverId").setValue("");
                        }
                        Toast.makeText(this, "Bus unassigned", Toast.LENGTH_SHORT).show();
                    } else {
                        Bus chosenBus = availableBuses.get(selected[0] - 1);
                        driver.setAssignedBusId(chosenBus.getBusId());
                        firebaseHelper.getDriversRef().child(driver.getUid()).child("assignedBusId").setValue(chosenBus.getBusId());
                        if (chosenBus.getBusId() != null) {
                            firebaseHelper.getBusesRef().child(chosenBus.getBusId()).child("driverId").setValue(driver.getUid());
                        }
                        Toast.makeText(this, "Assigned to " + chosenBus.getBusNumber(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteDriver(final Driver driver) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Driver")
                .setMessage("Are you sure you want to remove driver " + (driver.getName() != null ? driver.getName() : "") + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (driver.getUid() != null) {
                        firebaseHelper.getDriversRef().child(driver.getUid()).removeValue();
                        firebaseHelper.getUsersRef().child(driver.getUid()).removeValue();
                        Toast.makeText(this, "Driver removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (driversListener != null) firebaseHelper.getDriversRef().removeEventListener(driversListener);
        if (busesListener != null) firebaseHelper.getBusesRef().removeEventListener(busesListener);
    }
}
