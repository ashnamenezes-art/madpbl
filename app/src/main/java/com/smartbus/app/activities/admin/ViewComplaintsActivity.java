package com.smartbus.app.activities.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.ComplaintAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Complaint;
import com.smartbus.app.models.Feedback;
import com.smartbus.app.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewComplaintsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private View scrollFilterChips;
    private ChipGroup chipGroupStatus;
    private RecyclerView rvComplaints;
    private ProgressBar progressBar;
    private TextView tvNoComplaints;

    private ComplaintAdapter adapter;
    private final List<Complaint> allComplaints = new ArrayList<>();
    private final List<Complaint> filteredComplaints = new ArrayList<>();
    private final List<Complaint> feedbackAsComplaints = new ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private ValueEventListener complaintsListener;
    private ValueEventListener feedbackListener;

    private int currentTab = 0; // 0 = Complaints, 1 = Feedback
    private String currentStatusFilter = "all"; // all, pending, in_review, resolved

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_complaints);

        firebaseHelper = FirebaseHelper.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupTabsAndFilters();
        loadComplaints();
        loadFeedback();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        scrollFilterChips = findViewById(R.id.scroll_filter_chips);
        chipGroupStatus = findViewById(R.id.chip_group_status);
        rvComplaints = findViewById(R.id.rv_complaints);
        progressBar = findViewById(R.id.progress_bar);
        tvNoComplaints = findViewById(R.id.tv_no_complaints);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Complaints & Feedback");
        }
    }

    private void setupRecyclerView() {
        adapter = new ComplaintAdapter(this, filteredComplaints, this::showReplyDialog);
        rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        rvComplaints.setAdapter(adapter);
    }

    private void setupTabsAndFilters() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (currentTab == 0) {
                    scrollFilterChips.setVisibility(View.VISIBLE);
                    applyComplaintsFilter();
                } else {
                    scrollFilterChips.setVisibility(View.GONE);
                    displayFeedback();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_pending) {
                currentStatusFilter = Constants.COMPLAINT_PENDING;
            } else if (checkedId == R.id.chip_in_review) {
                currentStatusFilter = Constants.COMPLAINT_IN_REVIEW;
            } else if (checkedId == R.id.chip_resolved) {
                currentStatusFilter = Constants.COMPLAINT_RESOLVED;
            } else {
                currentStatusFilter = "all";
            }
            if (currentTab == 0) {
                applyComplaintsFilter();
            }
        });
    }

    private void loadComplaints() {
        progressBar.setVisibility(View.VISIBLE);

        complaintsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                allComplaints.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Complaint c = child.getValue(Complaint.class);
                    if (c != null) {
                        if (c.getComplaintId() == null) c.setComplaintId(child.getKey());
                        allComplaints.add(c);
                    }
                }

                Collections.sort(allComplaints, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

                if (currentTab == 0) {
                    applyComplaintsFilter();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ViewComplaintsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        firebaseHelper.getComplaintsRef().addValueEventListener(complaintsListener);
    }

    private void loadFeedback() {
        feedbackListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedbackAsComplaints.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Feedback fb = child.getValue(Feedback.class);
                    if (fb != null) {
                        String fbId = fb.getFeedbackId() != null ? fb.getFeedbackId() : child.getKey();
                        String subject = "Feedback (★ " + fb.getRating() + "/5)";
                        if (fb.getBusId() != null && !fb.getBusId().isEmpty()) {
                            subject += " - Bus: " + fb.getBusId();
                        }
                        String desc = fb.getComment() != null ? fb.getComment() : "No comment provided";
                        String status = fb.isResolved() ? Constants.COMPLAINT_RESOLVED : Constants.COMPLAINT_PENDING;

                        Complaint converted = new Complaint(
                                fbId,
                                fb.getPassengerId(),
                                fb.getBusId(),
                                subject,
                                desc,
                                fb.getTimestamp(),
                                status,
                                ""
                        );
                        feedbackAsComplaints.add(converted);
                    }
                }

                Collections.sort(feedbackAsComplaints, (c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

                if (currentTab == 1) {
                    displayFeedback();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        firebaseHelper.getFeedbackRef().addValueEventListener(feedbackListener);
    }

    private void applyComplaintsFilter() {
        filteredComplaints.clear();
        for (Complaint c : allComplaints) {
            if ("all".equalsIgnoreCase(currentStatusFilter)) {
                filteredComplaints.add(c);
            } else if (currentStatusFilter.equalsIgnoreCase(c.getStatus())) {
                filteredComplaints.add(c);
            }
        }

        adapter.setComplaints(filteredComplaints);

        if (filteredComplaints.isEmpty()) {
            tvNoComplaints.setText("No complaints found for this filter.");
            tvNoComplaints.setVisibility(View.VISIBLE);
            rvComplaints.setVisibility(View.GONE);
        } else {
            tvNoComplaints.setVisibility(View.GONE);
            rvComplaints.setVisibility(View.VISIBLE);
        }
    }

    private void displayFeedback() {
        adapter.setComplaints(feedbackAsComplaints);

        if (feedbackAsComplaints.isEmpty()) {
            tvNoComplaints.setText("No passenger feedback received yet.");
            tvNoComplaints.setVisibility(View.VISIBLE);
            rvComplaints.setVisibility(View.GONE);
        } else {
            tvNoComplaints.setVisibility(View.GONE);
            rvComplaints.setVisibility(View.VISIBLE);
        }
    }

    private void showReplyDialog(final Complaint complaint) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reply_complaint, null);
        TextView tvSubject = dialogView.findViewById(R.id.tv_dialog_complaint_subject);
        TextView tvDesc = dialogView.findViewById(R.id.tv_dialog_complaint_desc);
        EditText etReply = dialogView.findViewById(R.id.et_dialog_reply);

        tvSubject.setText(complaint.getSubject() != null ? complaint.getSubject() : "No Subject");
        tvDesc.setText(complaint.getDescription() != null ? complaint.getDescription() : "No Description");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Send Official Reply")
                .setView(dialogView)
                .setPositiveButton("Reply & Resolve", (dialog, which) -> {
                    String reply = etReply.getText() != null ? etReply.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(reply)) {
                        Toast.makeText(this, "Reply message cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (complaint.getComplaintId() != null) {
                        if (currentTab == 0) {
                            // Complaint
                            firebaseHelper.getComplaintsRef().child(complaint.getComplaintId()).child("adminReply").setValue(reply);
                            firebaseHelper.getComplaintsRef().child(complaint.getComplaintId()).child("status").setValue(Constants.COMPLAINT_RESOLVED);
                            Toast.makeText(this, "Complaint resolved", Toast.LENGTH_SHORT).show();
                        } else {
                            // Feedback
                            firebaseHelper.getFeedbackRef().child(complaint.getComplaintId()).child("isResolved").setValue(true);
                            Toast.makeText(this, "Feedback marked as acknowledged", Toast.LENGTH_SHORT).show();
                        }
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
        if (complaintsListener != null) firebaseHelper.getComplaintsRef().removeEventListener(complaintsListener);
        if (feedbackListener != null) firebaseHelper.getFeedbackRef().removeEventListener(feedbackListener);
    }
}
