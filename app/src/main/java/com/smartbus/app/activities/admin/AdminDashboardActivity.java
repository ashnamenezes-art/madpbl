package com.smartbus.app.activities.admin;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.activities.RoleSelectionActivity;
import com.smartbus.app.adapters.ComplaintAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Complaint;
import com.smartbus.app.models.Driver;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalBuses, tvActiveBuses, tvTotalDrivers, tvPendingComplaints;
    private MaterialCardView cardManageBuses, cardManageDrivers, cardManageRoutes, cardManageStops, cardLiveMap, cardComplaints;
    private RecyclerView rvRecentActivity;
    private TextView tvNoRecentActivity;

    private ComplaintAdapter complaintAdapter;
    private final List<Complaint> recentComplaintsList = new ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private SharedPrefManager prefManager;

    private ValueEventListener busesListener;
    private ValueEventListener driversListener;
    private ValueEventListener complaintsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        firebaseHelper = FirebaseHelper.getInstance();
        prefManager = SharedPrefManager.getInstance();

        initViews();
        setupToolbar();
        setupQuickActions();
        setupRecyclerView();
        attachFirebaseListeners();
    }

    private void initViews() {
        tvTotalBuses = findViewById(R.id.tv_total_buses);
        tvActiveBuses = findViewById(R.id.tv_active_buses);
        tvTotalDrivers = findViewById(R.id.tv_total_drivers);
        tvPendingComplaints = findViewById(R.id.tv_pending_complaints);

        cardManageBuses = findViewById(R.id.card_manage_buses);
        cardManageDrivers = findViewById(R.id.card_manage_drivers);
        cardManageRoutes = findViewById(R.id.card_manage_routes);
        cardManageStops = findViewById(R.id.card_manage_stops);
        cardLiveMap = findViewById(R.id.card_live_map);
        cardComplaints = findViewById(R.id.card_complaints);

        rvRecentActivity = findViewById(R.id.rv_recent_activity);
        tvNoRecentActivity = findViewById(R.id.tv_no_recent_activity);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }
    }

    private void setupQuickActions() {
        cardManageBuses.setOnClickListener(v -> startActivity(new Intent(this, ManageBusesActivity.class)));
        cardManageDrivers.setOnClickListener(v -> startActivity(new Intent(this, ManageDriversActivity.class)));
        cardManageRoutes.setOnClickListener(v -> startActivity(new Intent(this, ManageRoutesActivity.class)));
        cardManageStops.setOnClickListener(v -> startActivity(new Intent(this, ManageStopsActivity.class)));
        cardLiveMap.setOnClickListener(v -> startActivity(new Intent(this, AdminLiveMapActivity.class)));
        cardComplaints.setOnClickListener(v -> startActivity(new Intent(this, ViewComplaintsActivity.class)));
    }

    private void setupRecyclerView() {
        complaintAdapter = new ComplaintAdapter(this, recentComplaintsList, this::showReplyDialog);
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        rvRecentActivity.setAdapter(complaintAdapter);
    }

    private void attachFirebaseListeners() {
        // Buses count listener
        busesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalBuses = 0;
                int activeBuses = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    totalBuses++;
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null && bus.isActive()) {
                        activeBuses++;
                    }
                }
                tvTotalBuses.setText(String.valueOf(totalBuses));
                tvActiveBuses.setText(String.valueOf(activeBuses));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getBusesRef().addValueEventListener(busesListener);

        // Drivers count listener
        driversListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalDrivers = snapshot.getChildrenCount();
                tvTotalDrivers.setText(String.valueOf(totalDrivers));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getDriversRef().addValueEventListener(driversListener);

        // Complaints & Recent Activity listener
        complaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int pendingCount = 0;
                List<Complaint> allComplaints = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Complaint complaint = child.getValue(Complaint.class);
                    if (complaint != null) {
                        if (complaint.getComplaintId() == null) {
                            complaint.setComplaintId(child.getKey());
                        }
                        if (Constants.COMPLAINT_PENDING.equalsIgnoreCase(complaint.getStatus())) {
                            pendingCount++;
                        }
                        allComplaints.add(complaint);
                    }
                }

                tvPendingComplaints.setText(String.valueOf(pendingCount));

                // Sort complaints descending by timestamp
                Collections.sort(allComplaints, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

                recentComplaintsList.clear();
                int limit = Math.min(5, allComplaints.size());
                for (int i = 0; i < limit; i++) {
                    recentComplaintsList.add(allComplaints.get(i));
                }

                complaintAdapter.notifyDataSetChanged();

                if (recentComplaintsList.isEmpty()) {
                    tvNoRecentActivity.setVisibility(View.VISIBLE);
                    rvRecentActivity.setVisibility(View.GONE);
                } else {
                    tvNoRecentActivity.setVisibility(View.GONE);
                    rvRecentActivity.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        firebaseHelper.getComplaintsRef().addValueEventListener(complaintsListener);
    }

    private void showReplyDialog(Complaint complaint) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reply_complaint, null);
        TextView tvSubject = dialogView.findViewById(R.id.tv_dialog_complaint_subject);
        TextView tvDesc = dialogView.findViewById(R.id.tv_dialog_complaint_desc);
        EditText etReply = dialogView.findViewById(R.id.et_dialog_reply);

        tvSubject.setText(complaint.getSubject() != null ? complaint.getSubject() : "No Subject");
        tvDesc.setText(complaint.getDescription() != null ? complaint.getDescription() : "No Description");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reply to Complaint")
                .setView(dialogView)
                .setPositiveButton("Send & Resolve", (dialog, which) -> {
                    String reply = etReply.getText() != null ? etReply.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(reply)) {
                        Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (complaint.getComplaintId() != null) {
                        firebaseHelper.getComplaintsRef().child(complaint.getComplaintId()).child("adminReply").setValue(reply);
                        firebaseHelper.getComplaintsRef().child(complaint.getComplaintId()).child("status").setValue(Constants.COMPLAINT_RESOLVED);
                        Toast.makeText(this, "Complaint resolved with response", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out of the Admin Portal?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    prefManager.clearSession();
                    firebaseHelper.signOut();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, RoleSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (busesListener != null) {
            firebaseHelper.getBusesRef().removeEventListener(busesListener);
        }
        if (driversListener != null) {
            firebaseHelper.getDriversRef().removeEventListener(driversListener);
        }
        if (complaintsListener != null) {
            firebaseHelper.getComplaintsRef().removeEventListener(complaintsListener);
        }
    }
}
