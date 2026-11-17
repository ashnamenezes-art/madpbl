package com.smartbus.app.activities.driver;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.Driver;
import com.smartbus.app.models.User;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

public class DriverLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnDemoLogin;
    private ProgressBar progressBar;
    private TextView tvBackToRole;
    private ImageView ivBack;

    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = FirebaseHelper.getInstance();
        prefManager = SharedPrefManager.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnDemoLogin = findViewById(R.id.btnDemoLogin);
        progressBar = findViewById(R.id.progressBar);
        tvBackToRole = findViewById(R.id.tvBackToRole);
        ivBack = findViewById(R.id.ivBack);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        if (btnDemoLogin != null) {
            btnDemoLogin.setOnClickListener(v -> loginWithDemoDriver());
        }

        View.OnClickListener backListener = v -> finish();
        ivBack.setOnClickListener(backListener);
        tvBackToRole.setOnClickListener(backListener);
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_empty_email));
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_empty_password));
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        verifyDriverRole(auth.getCurrentUser().getUid(), email);
                    } else {
                        // If user doesn't exist in Firebase Auth yet, try creating it automatically
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, regTask -> {
                                    if (regTask.isSuccessful() && auth.getCurrentUser() != null) {
                                        String uid = auth.getCurrentUser().getUid();
                                        provisionAndLoginDriver(uid, email);
                                    } else {
                                        setLoading(false);
                                        String errorMsg = task.getException() != null
                                                ? task.getException().getMessage()
                                                : getString(R.string.error_generic);
                                        showError(errorMsg + "\nTip: Tap 'Quick Demo Login' to test immediately.");
                                    }
                                });
                    }
                });
    }

    private void verifyDriverRole(String uid, String email) {
        firebaseHelper.getUsersRef().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    provisionAndLoginDriver(uid, email);
                    return;
                }

                String role = snapshot.child("role").getValue(String.class);
                if (Constants.ROLE_DRIVER.equalsIgnoreCase(role)) {
                    loadDriverProfile(uid, email);
                } else {
                    setLoading(false);
                    auth.signOut();
                    showError("Access denied. This account has role '" + role + "', not driver.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                auth.signOut();
                showError("Database error: " + error.getMessage());
            }
        });
    }

    private void provisionAndLoginDriver(String uid, String email) {
        String name = "Driver " + (email.contains("@") ? email.substring(0, email.indexOf('@')) : "");
        User user = new User(uid, name, email, "", Constants.ROLE_DRIVER, "");
        firebaseHelper.getUsersRef().child(uid).setValue(user);

        // Assign to default sample bus BUS001 and route ROUTE001 if available
        Driver driver = new Driver(uid, name, email, "9876543210", "DL-PUNE-2024", "BUS001", "ROUTE001", false, "");
        firebaseHelper.getDriversRef().child(uid).setValue(driver);

        loadDriverProfile(uid, email);
    }

    private void loadDriverProfile(String uid, String fallbackEmail) {
        firebaseHelper.getDriversRef().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);

                Driver driver = snapshot.getValue(Driver.class);
                String name = "Driver";
                String phone = "";
                String busId = "BUS001";
                String routeId = "ROUTE001";
                String profileImage = null;

                if (driver != null) {
                    name = driver.getName() != null ? driver.getName() : "Driver";
                    phone = driver.getPhone() != null ? driver.getPhone() : "";
                    busId = driver.getAssignedBusId() != null ? driver.getAssignedBusId() : "BUS001";
                    routeId = driver.getAssignedRouteId() != null ? driver.getAssignedRouteId() : "ROUTE001";
                    profileImage = driver.getProfileImage();
                } else {
                    driver = new Driver(uid, name, fallbackEmail, "", "", busId, routeId, false, null);
                    firebaseHelper.getDriversRef().child(uid).setValue(driver);
                }

                User userSession = new User(uid, name, fallbackEmail, phone, Constants.ROLE_DRIVER, profileImage);
                prefManager.saveLoginSession(DriverLoginActivity.this, userSession);

                prefManager.setBusId(DriverLoginActivity.this, busId);
                prefManager.setRouteId(DriverLoginActivity.this, routeId);

                Toast.makeText(DriverLoginActivity.this, "Welcome, " + name, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(DriverLoginActivity.this, DriverHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showError("Failed to load driver profile: " + error.getMessage());
            }
        });
    }

    private void loginWithDemoDriver() {
        String demoUid = "DRV001";
        String demoName = "Rajesh Sharma (Driver)";
        String demoEmail = "driver@smartbus.com";
        String demoPhone = "+91 98230 12345";
        String busId = "BUS001";
        String routeId = "ROUTE001";

        User userSession = new User(demoUid, demoName, demoEmail, demoPhone, Constants.ROLE_DRIVER, "");
        prefManager.saveLoginSession(this, userSession);
        prefManager.setBusId(this, busId);
        prefManager.setRouteId(this, routeId);

        Toast.makeText(this, "Logged in as Demo Driver (" + demoName + ")", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, DriverHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        if (btnDemoLogin != null) {
            btnDemoLogin.setEnabled(!isLoading);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
