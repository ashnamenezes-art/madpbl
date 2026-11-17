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
import com.google.firebase.database.FirebaseDatabase;
import com.smartbus.app.R;
import com.smartbus.app.models.User;
import com.smartbus.app.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

public class PassengerRegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private ProgressDialog progressDialog;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_register);

        auth = FirebaseAuth.getInstance();

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegister());

        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) { tilName.setError(getString(R.string.error_empty_name)); return; }
        if (TextUtils.isEmpty(email)) { tilEmail.setError(getString(R.string.error_empty_email)); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { tilEmail.setError(getString(R.string.error_invalid_email)); return; }
        if (TextUtils.isEmpty(phone)) { tilPhone.setError(getString(R.string.error_empty_phone)); return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError(getString(R.string.error_empty_password)); return; }
        if (password.length() < 6) { tilPassword.setError(getString(R.string.error_short_password)); return; }
        if (!password.equals(confirmPassword)) { tilConfirmPassword.setError(getString(R.string.error_password_mismatch)); return; }

        progressDialog.show();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = auth.getCurrentUser().getUid();
                        saveUserToDatabase(uid, name, email, phone);
                    } else {
                        progressDialog.dismiss();
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_generic);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String name, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("role", "passenger");
        userData.put("profileImage", "");

        // Save to users/{uid}
        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .setValue(userData)
                .addOnCompleteListener(task -> {
                    // Also save to passengers/{uid}
                    FirebaseDatabase.getInstance().getReference("passengers").child(uid)
                            .setValue(userData)
                            .addOnCompleteListener(t -> {
                                progressDialog.dismiss();
                                SharedPrefManager.getInstance(this)
                                        .saveUserSession(uid, name, email, phone, "passenger");
                                startActivity(new Intent(this, PassengerHomeActivity.class));
                                finishAffinity();
                            });
                });
    }
}
