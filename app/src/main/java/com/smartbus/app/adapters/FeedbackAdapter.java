package com.smartbus.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartbus.app.R;
import com.smartbus.app.models.Feedback;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.FeedbackViewHolder> {

    private final List<Feedback> feedbackList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

    public FeedbackAdapter(List<Feedback> feedbackList) {
        this.feedbackList = feedbackList;
    }

    @NonNull
    @Override
    public FeedbackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feedback, parent, false);
        return new FeedbackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackViewHolder holder, int position) {
        Feedback fb = feedbackList.get(position);
        if (holder.tvFeedbackBus != null) {
            holder.tvFeedbackBus.setText(fb.getBusId() != null ? "Bus: " + fb.getBusId() : "Bus Feedback");
        }
        if (holder.itemRatingBar != null) {
            holder.itemRatingBar.setRating(fb.getRating());
        }
        if (holder.tvFeedbackComment != null) {
            holder.tvFeedbackComment.setText(fb.getComment() != null ? fb.getComment() : "");
        }
        if (holder.tvFeedbackDate != null) {
            long time = fb.getTimestamp() > 0 ? fb.getTimestamp() : System.currentTimeMillis();
            holder.tvFeedbackDate.setText(dateFormat.format(new Date(time)));
        }
    }

    @Override
    public int getItemCount() {
        return feedbackList != null ? feedbackList.size() : 0;
    }

    static class FeedbackViewHolder extends RecyclerView.ViewHolder {
        TextView tvFeedbackBus, tvFeedbackComment, tvFeedbackDate;
        RatingBar itemRatingBar;

        FeedbackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFeedbackBus = itemView.findViewById(R.id.tvFeedbackBus);
            tvFeedbackComment = itemView.findViewById(R.id.tvFeedbackComment);
            tvFeedbackDate = itemView.findViewById(R.id.tvFeedbackDate);
            itemRatingBar = itemView.findViewById(R.id.itemRatingBar);
        }
    }
}
