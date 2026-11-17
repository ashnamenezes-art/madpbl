package com.smartbus.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.smartbus.app.R;
import com.smartbus.app.models.Driver;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminDriverAdapter extends RecyclerView.Adapter<AdminDriverAdapter.AdminDriverViewHolder> {

    public interface OnAdminDriverActionListener {
        void onDriverClick(Driver driver);
        void onEditClick(Driver driver);
        void onDeleteClick(Driver driver);
        void onAssignBusClick(Driver driver);
    }

    private final Context context;
    private List<Driver> driverList;
    private OnAdminDriverActionListener listener;

    public AdminDriverAdapter(Context context) {
        this.context = context;
        this.driverList = new ArrayList<>();
    }

    public AdminDriverAdapter(Context context, List<Driver> driverList, OnAdminDriverActionListener listener) {
        this.context = context;
        this.driverList = driverList != null ? driverList : new ArrayList<>();
        this.listener = listener;
    }

    public void setDrivers(List<Driver> drivers) {
        this.driverList = drivers != null ? drivers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnAdminDriverActionListener(OnAdminDriverActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminDriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver, parent, false);
        return new AdminDriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminDriverViewHolder holder, int position) {
        Driver driver = driverList.get(position);
        holder.bind(driver);
    }

    @Override
    public int getItemCount() {
        return driverList != null ? driverList.size() : 0;
    }

    class AdminDriverViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvPhone;
        private final TextView tvEmail;
        private final TextView tvLicense;
        private final TextView tvBus;
        private final View viewDutyDot;
        private final TextView tvDutyStatusLabel;
        private final ImageView btnEdit;
        private final ImageView btnDelete;
        private final MaterialButton btnAssignBus;

        public AdminDriverViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivDriverItemAvatar);
            tvName = itemView.findViewById(R.id.tvDriverItemName);
            tvPhone = itemView.findViewById(R.id.tvDriverItemPhone);
            tvEmail = itemView.findViewById(R.id.tv_driver_email);
            tvLicense = itemView.findViewById(R.id.tv_driver_license);
            tvBus = itemView.findViewById(R.id.tvDriverItemBus);
            viewDutyDot = itemView.findViewById(R.id.viewDutyDot);
            tvDutyStatusLabel = itemView.findViewById(R.id.tvDutyStatusLabel);
            btnEdit = itemView.findViewById(R.id.btn_edit_driver);
            btnDelete = itemView.findViewById(R.id.btn_delete_driver);
            btnAssignBus = itemView.findViewById(R.id.btn_assign_bus);
        }

        public void bind(final Driver driver) {
            tvName.setText(driver.getName() != null && !driver.getName().isEmpty()
                    ? driver.getName() : "Unnamed Driver");

            tvPhone.setText(driver.getPhone() != null && !driver.getPhone().isEmpty()
                    ? ("Phone: " + driver.getPhone()) : "Phone: Not provided");

            if (tvEmail != null) {
                tvEmail.setText(driver.getEmail() != null && !driver.getEmail().isEmpty()
                        ? ("Email: " + driver.getEmail()) : "Email: Not provided");
            }

            if (tvLicense != null) {
                tvLicense.setText(driver.getLicenseNumber() != null && !driver.getLicenseNumber().isEmpty()
                        ? ("License: " + driver.getLicenseNumber()) : "License: Not provided");
            }

            if (driver.getAssignedBusId() != null && !driver.getAssignedBusId().isEmpty()) {
                tvBus.setText("Assigned Bus: " + driver.getAssignedBusId());
            } else {
                tvBus.setText("Assigned: None");
            }

            if (driver.isOnDuty()) {
                if (viewDutyDot != null) viewDutyDot.setBackgroundResource(R.drawable.bg_status_active);
                if (tvDutyStatusLabel != null) {
                    tvDutyStatusLabel.setText("On Duty");
                    tvDutyStatusLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.driver_color));
                }
            } else {
                if (viewDutyDot != null) viewDutyDot.setBackgroundResource(R.drawable.bg_status_inactive);
                if (tvDutyStatusLabel != null) {
                    tvDutyStatusLabel.setText("Off Duty");
                    tvDutyStatusLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                }
            }

            if (driver.getProfileImage() != null && !driver.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(driver.getProfileImage())
                        .placeholder(R.drawable.ic_driver)
                        .error(R.drawable.ic_driver)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_driver);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDriverClick(driver);
                }
            });

            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onEditClick(driver);
                    }
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClick(driver);
                    }
                });
            }

            if (btnAssignBus != null) {
                btnAssignBus.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAssignBusClick(driver);
                    }
                });
            }
        }
    }
}
