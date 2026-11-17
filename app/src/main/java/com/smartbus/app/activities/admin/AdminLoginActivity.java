package com.smartbus.app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.activities.RoleSelectionActivity;
import com.smartbus.app.database.FirebaseHelper;
import com.smartbus.app.models.User;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnDemoLogin;
    private ProgressBar progressBar;
    private TextView tvBackToRole;

    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        firebaseHelper = FirebaseHelper.getInstance();
        auth = firebaseHelper.getAuth();
        prefManager = SharedPrefManager.getInstance();

        // Check if already logged in as admin
        if (prefManager.isLoggedIn(this) && Constants.ROLE_ADMIN.equalsIgnoreCase(prefManager.getUserRole(this))) {
            navigateToDashboard();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnDemoLogin = findViewById(R.id.btn_demo_login);
        progressBar = findViewById(R.id.progress_bar);
        tvBackToRole = findViewById(R.id.tv_back_to_role);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        if (btnDemoLogin != null) {
            btnDemoLogin.setOnClickListener(v -> loginWithDemoAdmin());
        }

        tvBackToRole.setOnClickListener(v -> {
            Intent intent = new Intent(AdminLoginActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_email));
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_password));
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_short_password));
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            verifyAdminRole(firebaseUser);
                        } else {
                            setLoading(false);
                            Toast.makeText(AdminLoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // If user doesn't exist, try auto-registering admin
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this, regTask -> {
                                    if (regTask.isSuccessful() && auth.getCurrentUser() != null) {
                                        FirebaseUser firebaseUser = auth.getCurrentUser();
                                        User adminUser = new User(firebaseUser.getUid(), "Administrator", email, "", Constants.ROLE_ADMIN, "");
                                        firebaseHelper.getUsersRef().child(firebaseUser.getUid()).setValue(adminUser);
                                        prefManager.saveLoginSession(AdminLoginActivity.this, adminUser);
                                        setLoading(false);
                                        Toast.makeText(AdminLoginActivity.this, "Admin account registered & logged in!", Toast.LENGTH_SHORT).show();
                                        navigateToDashboard();
                                    } else {
                                        setLoading(false);
                                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                                        Toast.makeText(AdminLoginActivity.this, "Login error: " + errorMsg + "\nTip: Tap 'Quick Demo Login' to test immediately.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    private void verifyAdminRole(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        firebaseHelper.getUsersRef().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                setLoading(false);
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && Constants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
                        prefManager.saveLoginSession(AdminLoginActivity.this, user);
                        Toast.makeText(AdminLoginActivity.this, "Welcome, Admin!", Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                        return;
                    }
                }

                // If user doesn't exist in DB or email contains admin:
                String email = firebaseUser.getEmail();
                User adminUser = new User(uid, "Administrator", email != null ? email : "admin@smartbus.com", "", Constants.ROLE_ADMIN, "");
                firebaseHelper.getUsersRef().child(uid).setValue(adminUser);
                prefManager.saveLoginSession(AdminLoginActivity.this, adminUser);
                Toast.makeText(AdminLoginActivity.this, "Welcome, Admin!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                setLoading(false);
                Toast.makeText(AdminLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithDemoAdmin() {
        String demoUid = "ADM001";
        String demoName = "System Administrator";
        String demoEmail = "admin@smartbus.com";

        User adminUser = new User(demoUid, demoName, demoEmail, "", Constants.ROLE_ADMIN, "");
        prefManager.saveLoginSession(this, adminUser);
        Toast.makeText(this, "Logged in as Demo Administrator", Toast.LENGTH_SHORT).show();
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
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
}
