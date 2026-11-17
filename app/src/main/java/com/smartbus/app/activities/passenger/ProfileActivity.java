package com.smartbus.app.activities.passenger;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.activities.RoleSelectionActivity;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.User;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvTripsCount, tvComplaintsCount, tvRatingGiven;
    private ImageButton btnBack;
    private CardView menuEditProfile, menuNotifications, menuHelp, menuPrivacy, menuLogout;

    private FirebaseHelper firebaseHelper;
    private SharedPrefManager sharedPrefManager;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseHelper = FirebaseHelper.getInstance();
        sharedPrefManager = SharedPrefManager.getInstance(this);

        uid = sharedPrefManager.getUserId();
        if (uid == null) {
            uid = firebaseHelper.getCurrentUid();
        }

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvTripsCount = findViewById(R.id.tvTripsCount);
        tvComplaintsCount = findViewById(R.id.tvComplaintsCount);
        tvRatingGiven = findViewById(R.id.tvRatingGiven);

        menuEditProfile = findViewById(R.id.menuEditProfile);
        menuNotifications = findViewById(R.id.menuNotifications);
        menuHelp = findViewById(R.id.menuHelp);
        menuPrivacy = findViewById(R.id.menuPrivacy);
        menuLogout = findViewById(R.id.menuLogout);

        // Populate initial from preferences
        if (tvName != null) tvName.setText(sharedPrefManager.getUserName());
        if (tvEmail != null) tvEmail.setText(sharedPrefManager.getUserEmail());

        setupListeners();
        loadProfileData();
        loadStats();
    }

    private void setupListeners() {
        if (menuEditProfile != null) {
            menuEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        if (menuNotifications != null) {
            menuNotifications.setOnClickListener(v ->
                    Toast.makeText(this, "Push notifications are enabled", Toast.LENGTH_SHORT).show());
        }

        if (menuHelp != null) {
            menuHelp.setOnClickListener(v ->
                    Toast.makeText(this, "For assistance contact support@smartbus.com or 1800-SMARTBUS", Toast.LENGTH_LONG).show());
        }

        if (menuPrivacy != null) {
            menuPrivacy.setOnClickListener(v ->
                    Toast.makeText(this, "Smart Bus Privacy Policy: Your data and location are encrypted and secure.", Toast.LENGTH_LONG).show());
        }

        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> confirmLogout());
        }
    }

    private void loadProfileData() {
        if (uid == null) return;

        firebaseHelper.getUsersRef().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (tvName != null && user.getName() != null) {
                        tvName.setText(user.getName());
                    }
                    if (tvEmail != null && user.getEmail() != null) {
                        tvEmail.setText(user.getEmail());
                    }
                    sharedPrefManager.saveUserSession(uid, user.getName(), user.getEmail(), user.getPhone(), user.getRole());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStats() {
        if (uid == null) return;

        // Trips count
        firebaseHelper.getTicketsRef().orderByChild("passengerId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (tvTripsCount != null) {
                            tvTripsCount.setText(String.valueOf(count));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Complaints count
        firebaseHelper.getComplaintsRef().orderByChild("passengerId").equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        if (tvComplaintsCount != null) {
                            tvComplaintsCount.setText(String.valueOf(count));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile");

        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null);
        // Simple programmatically created layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etName = new EditText(this);
        etName.setHint("Full Name");
        etName.setText(sharedPrefManager.getUserName());
        layout.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint("Phone Number");
        etPhone.setText(sharedPrefManager.getUserPhone());
        layout.addView(etPhone);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (uid != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", newName);
                updates.put("phone", newPhone);

                firebaseHelper.getUsersRef().child(uid).updateChildren(updates);
                firebaseHelper.getPassengersRef().child(uid).updateChildren(updates);

                sharedPrefManager.saveUserSession(uid, newName, sharedPrefManager.getUserEmail(), newPhone, sharedPrefManager.getUserRole());
                if (tvName != null) tvName.setText(newName);
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    firebaseHelper.signOut();
                    sharedPrefManager.clearSession();
                    Intent intent = new Intent(ProfileActivity.this, RoleSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
