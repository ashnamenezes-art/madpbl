package com.smartbus.app.activities.passenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.smartbus.app.R;
import com.smartbus.app.utils.SharedPrefManager;

public class PassengerLoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private ProgressDialog progressDialog;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_login);

        auth = FirebaseAuth.getInstance();
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        findViewById(R.id.btnLogin).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.btnDemoLogin).setOnClickListener(v -> loginWithDemoPassenger());

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());

        TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, PassengerRegisterActivity.class)));
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_password));
            return;
        }

        progressDialog.show();
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        checkUserRole(uid, email);
                    } else {
                        // If user doesn't exist in Firebase Auth yet, try auto-registering
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(regTask -> {
                                    if (regTask.isSuccessful() && auth.getCurrentUser() != null) {
                                        String uid = auth.getCurrentUser().getUid();
                                        provisionPassenger(uid, email);
                                    } else {
                                        progressDialog.dismiss();
                                        String msg = task.getException() != null
                                                ? task.getException().getMessage()
                                                : getString(R.string.error_generic);
                                        Toast.makeText(this, msg + "\nTip: Tap 'Quick Demo Login' to test immediately.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    private void provisionPassenger(String uid, String email) {
        String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Passenger";
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.child("uid").setValue(uid);
        userRef.child("name").setValue(name);
        userRef.child("email").setValue(email);
        userRef.child("role").setValue("passenger");

        progressDialog.dismiss();
        SharedPrefManager.getInstance(this).saveUserSession(uid, name, email, "", "passenger");
        startActivity(new Intent(this, PassengerHomeActivity.class));
        finish();
    }

    private void checkUserRole(String uid, String email) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                progressDialog.dismiss();
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if ("passenger".equals(role)) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        SharedPrefManager.getInstance(PassengerLoginActivity.this)
                                .saveUserSession(uid, name != null ? name : "Passenger", email, phone != null ? phone : "", role);
                        startActivity(new Intent(PassengerLoginActivity.this, PassengerHomeActivity.class));
                        finish();
                    } else {
                        auth.signOut();
                        Toast.makeText(PassengerLoginActivity.this,
                                "Access denied. This account has role '" + role + "', not passenger.",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    provisionPassenger(uid, email);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(PassengerLoginActivity.this,
                        getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithDemoPassenger() {
        String demoUid = "PSG001";
        String demoName = "Aarav Sharma";
        String demoEmail = "passenger@smartbus.com";
        String demoPhone = "+91 98765 43210";

        SharedPrefManager.getInstance(this).saveUserSession(demoUid, demoName, demoEmail, demoPhone, "passenger");
        Toast.makeText(this, "Logged in as Demo Passenger (" + demoName + ")", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, PassengerHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void sendPasswordReset() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Enter email to reset password");
            return;
        }
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, getString(R.string.reset_email_sent),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
