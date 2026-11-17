package com.smartbus.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartbus.app.R;
import com.smartbus.app.models.Complaint;
import com.smartbus.app.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder> {

    public interface OnReplyClickListener {
        void onReplyClick(Complaint complaint);
    }

    private final Context context;
    private List<Complaint> complaintList;
    private OnReplyClickListener replyClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public ComplaintAdapter(Context context) {
        this.context = context;
        this.complaintList = new ArrayList<>();
    }

    public ComplaintAdapter(Context context, List<Complaint> complaints, OnReplyClickListener listener) {
        this.context = context;
        this.complaintList = complaints != null ? complaints : new ArrayList<>();
        this.replyClickListener = listener;
    }

    public void setComplaints(List<Complaint> complaints) {
        this.complaintList = complaints != null ? complaints : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaintList.get(position);
        holder.bind(complaint);
    }

    @Override
    public int getItemCount() {
        return complaintList != null ? complaintList.size() : 0;
    }

    class ComplaintViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvPassengerName;
        private final TextView tvTimestamp;
        private final TextView tvSubject;
        private final TextView tvDescription;
        private final TextView tvStatusChip;
        private final MaterialButton btnReply;
        private final View layoutAdminReply;
        private final TextView tvAdminReply;
        private final RatingBar ratingBar;

        public ComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerName = itemView.findViewById(R.id.tv_passenger_name);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvStatusChip = itemView.findViewById(R.id.tv_status_chip);
            btnReply = itemView.findViewById(R.id.btn_reply);
            layoutAdminReply = itemView.findViewById(R.id.layout_admin_reply);
            tvAdminReply = itemView.findViewById(R.id.tv_admin_reply);
            ratingBar = itemView.findViewById(R.id.rating_bar);
        }

        public void bind(final Complaint complaint) {
            if (complaint.getPassengerId() != null && !complaint.getPassengerId().isEmpty()) {
                tvPassengerName.setText("Passenger: " + complaint.getPassengerId());
            } else {
                tvPassengerName.setText("Anonymous Passenger");
            }

            if (complaint.getTimestamp() > 0) {
                tvTimestamp.setText(dateFormat.format(new Date(complaint.getTimestamp())));
            } else {
                tvTimestamp.setText("Recently");
            }

            tvSubject.setText(complaint.getSubject() != null ? complaint.getSubject() : "No Subject");
            tvDescription.setText(complaint.getDescription() != null ? complaint.getDescription() : "No Description");

            String status = complaint.getStatus() != null ? complaint.getStatus().toLowerCase() : Constants.COMPLAINT_PENDING;

            if (Constants.COMPLAINT_RESOLVED.equals(status)) {
                tvStatusChip.setText("Resolved");
                tvStatusChip.setTextColor(Color.parseColor("#2E7D32"));
                tvStatusChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                btnReply.setVisibility(View.GONE);
            } else if (Constants.COMPLAINT_IN_REVIEW.equals(status)) {
                tvStatusChip.setText("In Review");
                tvStatusChip.setTextColor(Color.parseColor("#1565C0"));
                tvStatusChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                btnReply.setVisibility(View.VISIBLE);
            } else {
                tvStatusChip.setText("Pending");
                tvStatusChip.setTextColor(Color.parseColor("#E65100"));
                tvStatusChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                btnReply.setVisibility(View.VISIBLE);
            }

            if (complaint.getAdminReply() != null && !complaint.getAdminReply().trim().isEmpty()) {
                layoutAdminReply.setVisibility(View.VISIBLE);
                tvAdminReply.setText(complaint.getAdminReply());
            } else {
                layoutAdminReply.setVisibility(View.GONE);
            }

            btnReply.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    replyClickListener.onReplyClick(complaint);
                }
            });
        }
    }
}
