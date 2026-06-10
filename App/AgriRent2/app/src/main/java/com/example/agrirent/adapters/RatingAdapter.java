package com.example.agrirent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.agrirent.R;
import com.example.agrirent.models.RatingResponse;
import java.util.List;

public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.RatingViewHolder> {

    private List<RatingResponse> ratings;

    public RatingAdapter(List<RatingResponse> ratings) {
        this.ratings = ratings;
    }

    public void updateRatings(List<RatingResponse> newRatings) {
        this.ratings = newRatings;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RatingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new RatingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RatingViewHolder holder, int position) {
        RatingResponse rating = ratings.get(position);
        
        holder.tvReviewerName.setText(rating.getUserFullName() != null ? rating.getUserFullName() : "Anonymous");
        if (rating.getUserFullName() != null && !rating.getUserFullName().isEmpty()) {
            holder.tvReviewerAvatar.setText(String.valueOf(rating.getUserFullName().charAt(0)).toUpperCase());
        } else {
            holder.tvReviewerAvatar.setText("A");
        }
        
        holder.ratingBarReview.setRating((float) rating.getRatingValue());
        
        if (rating.getComment() != null && !rating.getComment().trim().isEmpty()) {
            holder.tvReviewComment.setText(rating.getComment().trim());
            holder.tvReviewComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvReviewComment.setVisibility(View.GONE);
        }
        
        // Formatting simple date roughly for display
        if (rating.getCreatedAt() != null && rating.getCreatedAt().length() >= 10) {
            holder.tvReviewDate.setText(rating.getCreatedAt().substring(0, 10));
        } else {
            holder.tvReviewDate.setText("");
        }

        if (rating.getTargetName() != null && !rating.getTargetName().isEmpty()) {
            holder.tvItemName.setText(rating.getTargetType() + ": " + rating.getTargetName());
            holder.tvItemName.setVisibility(View.VISIBLE);
        } else {
            holder.tvItemName.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return ratings == null ? 0 : ratings.size();
    }

    static class RatingViewHolder extends RecyclerView.ViewHolder {
        TextView tvReviewerAvatar, tvReviewerName, tvReviewDate, tvReviewComment, tvItemName;
        RatingBar ratingBarReview;

        RatingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerAvatar = itemView.findViewById(R.id.tvReviewerAvatar);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            tvReviewComment = itemView.findViewById(R.id.tvReviewComment);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);
        }
    }
}
