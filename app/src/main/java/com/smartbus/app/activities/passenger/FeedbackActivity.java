package com.smartbus.app.activities.passenger;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.adapters.ComplaintAdapter;
import com.smartbus.app.adapters.FeedbackAdapter;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Bus;
import com.smartbus.app.models.Complaint;
import com.smartbus.app.models.Feedback;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeedbackActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private CardView cardFeedback, cardComplaints;
    private RatingBar ratingBar;
    private Spinner spinnerBus;
    private EditText etFeedbackComment, etComplaintSubject, etComplaintDescription;
    private Button btnSubmitFeedback, btnSubmitComplaint;
    private RecyclerView rvPastSubmissions;

    private List<Feedback> feedbackList = new ArrayList<>();
    private List<Complaint> complaintList = new ArrayList<>();
    private FeedbackAdapter feedbackAdapter;
    private ComplaintAdapter complaintAdapter;

    private List<String> busDisplayList = new ArrayList<>();
    private List<String> busIdList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private FirebaseHelper firebaseHelper;
    private String passengerId;
    private boolean isFeedbackTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        firebaseHelper = FirebaseHelper.getInstance();
        passengerId = SharedPrefManager.getUserId(this);
        if (passengerId == null) {
            passengerId = firebaseHelper.getCurrentUid();
        }
        if (passengerId == null) {
            passengerId = "demo_passenger";
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tabLayout = findViewById(R.id.tabLayout);
        cardFeedback = findViewById(R.id.cardFeedback);
        cardComplaints = findViewById(R.id.cardComplaints);
        ratingBar = findViewById(R.id.ratingBar);
        spinnerBus = findViewById(R.id.spinnerBus);
        etFeedbackComment = findViewById(R.id.etFeedbackComment);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);

        etComplaintSubject = findViewById(R.id.etComplaintSubject);
        etComplaintDescription = findViewById(R.id.etComplaintDescription);
        btnSubmitComplaint = findViewById(R.id.btnSubmitComplaint);

        rvPastSubmissions = findViewById(R.id.rvPastFeedback);
        rvPastSubmissions.setLayoutManager(new LinearLayoutManager(this));

        feedbackAdapter = new FeedbackAdapter(feedbackList);
        complaintAdapter = new ComplaintAdapter(this, complaintList, null);
        rvPastSubmissions.setAdapter(feedbackAdapter);

        setupBusSpinner();
        setupTabs();

        btnSubmitFeedback.setOnClickListener(v -> submitFeedback());
        btnSubmitComplaint.setOnClickListener(v -> submitComplaint());

        loadPastFeedbacks();
        loadPastComplaints();
    }

    private void setupBusSpinner() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, busDisplayList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBus.setAdapter(spinnerAdapter);

        firebaseHelper.getBusesRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                busDisplayList.clear();
                busIdList.clear();
                busDisplayList.add("General / Entire Service");
                busIdList.add("general");

                for (DataSnapshot child : snapshot.getChildren()) {
                    Bus bus = child.getValue(Bus.class);
                    if (bus != null) {
                        busDisplayList.add("Bus #" + bus.getBusNumber() + " - " + (bus.getBusName() != null ? bus.getBusName() : ""));
                        busIdList.add(bus.getBusId());
                    }
                }
                spinnerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    isFeedbackTab = true;
                    cardFeedback.setVisibility(View.VISIBLE);
                    cardComplaints.setVisibility(View.GONE);
                    rvPastSubmissions.setAdapter(feedbackAdapter);
                } else {
                    isFeedbackTab = false;
                    cardFeedback.setVisibility(View.GONE);
                    cardComplaints.setVisibility(View.VISIBLE);
                    rvPastSubmissions.setAdapter(complaintAdapter);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void submitFeedback() {
        float rating = ratingBar.getRating();
        String comment = etFeedbackComment.getText().toString().trim();
        int selectedBusIndex = spinnerBus.getSelectedItemPosition();
        String busId = (selectedBusIndex >= 0 && selectedBusIndex < busIdList.size()) ? busIdList.get(selectedBusIndex) : "general";

        if (TextUtils.isEmpty(comment)) {
            etFeedbackComment.setError("Please enter your comments");
            return;
        }

        String feedbackId = "FB_" + UUID.randomUUID().toString().substring(0, 8);
        Feedback fb = new Feedback(feedbackId, passengerId, busId, "", rating, comment, System.currentTimeMillis(), true);

        firebaseHelper.getFeedbackRef().child(feedbackId).setValue(fb)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    etFeedbackComment.setText("");
                    ratingBar.setRating(5.0f);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void submitComplaint() {
        String subject = etComplaintSubject.getText().toString().trim();
        String description = etComplaintDescription.getText().toString().trim();
        int selectedBusIndex = spinnerBus.getSelectedItemPosition();
        String busId = (selectedBusIndex >= 0 && selectedBusIndex < busIdList.size()) ? busIdList.get(selectedBusIndex) : "general";

        if (TextUtils.isEmpty(subject)) {
            etComplaintSubject.setError("Please enter a subject");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            etComplaintDescription.setError("Please describe your complaint");
            return;
        }

        String complaintId = "CMP_" + UUID.randomUUID().toString().substring(0, 8);
        Complaint complaint = new Complaint(complaintId, passengerId, busId, subject, description, System.currentTimeMillis(), Constants.COMPLAINT_PENDING, "");

        firebaseHelper.getComplaintsRef().child(complaintId).setValue(complaint)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Complaint registered successfully. ID: " + complaintId, Toast.LENGTH_LONG).show();
                    etComplaintSubject.setText("");
                    etComplaintDescription.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadPastFeedbacks() {
        firebaseHelper.getFeedbackRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedbackList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Feedback fb = child.getValue(Feedback.class);
                    if (fb != null && passengerId.equals(fb.getPassengerId())) {
                        feedbackList.add(0, fb);
                    }
                }
                if (isFeedbackTab) {
                    feedbackAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPastComplaints() {
        firebaseHelper.getComplaintsRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                complaintList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Complaint c = child.getValue(Complaint.class);
                    if (c != null && passengerId.equals(c.getPassengerId())) {
                        complaintList.add(0, c);
                    }
                }
                if (!isFeedbackTab) {
                    complaintAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
