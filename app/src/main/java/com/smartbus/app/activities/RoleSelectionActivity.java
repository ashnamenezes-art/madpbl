package com.smartbus.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.smartbus.app.R;
import com.smartbus.app.activities.admin.AdminLoginActivity;
import com.smartbus.app.activities.driver.DriverLoginActivity;
import com.smartbus.app.activities.passenger.PassengerLoginActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        CardView cardPassenger = findViewById(R.id.cardPassenger);
        CardView cardDriver = findViewById(R.id.cardDriver);
        CardView cardAdmin = findViewById(R.id.cardAdmin);

        if (cardPassenger != null) {
            cardPassenger.setOnClickListener(v ->
                    startActivity(new Intent(this, PassengerLoginActivity.class)));
        }

        if (cardDriver != null) {
            cardDriver.setOnClickListener(v ->
                    startActivity(new Intent(this, DriverLoginActivity.class)));
        }

        if (cardAdmin != null) {
            cardAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminLoginActivity.class)));
        }
    }
}
