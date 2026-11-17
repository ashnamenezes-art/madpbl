package com.smartbus.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.smartbus.app.R;
import com.smartbus.app.activities.admin.AdminDashboardActivity;
import com.smartbus.app.activities.driver.DriverHomeActivity;
import com.smartbus.app.activities.passenger.PassengerHomeActivity;
import com.smartbus.app.utils.Constants;
import com.smartbus.app.utils.SharedPrefManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo fade-in
        View layoutContent = findViewById(R.id.layoutContent);
        if (layoutContent != null) {
            layoutContent.setAlpha(0f);
            layoutContent.animate()
                    .alpha(1f)
                    .setDuration(800)
                    .start();
        }

        // Delay 2 seconds then navigate
        new Handler().postDelayed(() -> {
            SharedPrefManager spm = SharedPrefManager.getInstance(SplashActivity.this);
            if (spm.isLoggedIn()) {
                String role = spm.getUserRole();
                if (Constants.ROLE_PASSENGER.equalsIgnoreCase(role)) {
                    startActivity(new Intent(this, PassengerHomeActivity.class));
                } else if (Constants.ROLE_DRIVER.equalsIgnoreCase(role)) {
                    startActivity(new Intent(this, DriverHomeActivity.class));
                } else if (Constants.ROLE_ADMIN.equalsIgnoreCase(role)) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(this, RoleSelectionActivity.class));
                }
            } else {
                startActivity(new Intent(this, RoleSelectionActivity.class));
            }
            finish();
        }, 2000);
    }
}
