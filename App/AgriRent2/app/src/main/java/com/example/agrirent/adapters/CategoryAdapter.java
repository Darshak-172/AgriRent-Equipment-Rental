package com.example.agrirent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.models.ApiCategory;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private Context context;
    private List<ApiCategory> categories;
    private int selectedPosition = 0;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(int position, ApiCategory category);
    }

    public CategoryAdapter(Context context, List<ApiCategory> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_pill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApiCategory category = categories.get(position);
        holder.tvName.setText(category.getName());

        // Show emoji icon based on name (no icon resource needed)
        String emoji = getCategoryEmoji(category.getName());
        holder.tvEmoji.setText(emoji);

        boolean isSelected = (position == selectedPosition);
        holder.itemView.setSelected(isSelected);
        holder.tvName.setSelected(isSelected);

        holder.itemView.setOnClickListener(v -> {
            int newPos = holder.getAdapterPosition();
            if (newPos == RecyclerView.NO_POSITION || newPos >= categories.size()) {
                return;
            }

            int previousPos = selectedPosition;
            selectedPosition = newPos;
            if (previousPos >= 0 && previousPos < categories.size()) {
                notifyItemChanged(previousPos);
            }
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCategoryClick(selectedPosition, categories.get(selectedPosition));
            }
        });
    }

    private String getCategoryEmoji(String name) {
        if (name == null) return "🌾";
        String lower = name.toLowerCase();
        if (lower.contains("tractor") || lower.contains("equipment")) return "🚜";
        if (lower.contains("seed") || lower.contains("crop")) return "🌱";
        if (lower.contains("irrigation") || lower.contains("water")) return "💧";
        if (lower.contains("harvest")) return "🌾";
        if (lower.contains("pesticide") || lower.contains("chemical")) return "🧪";
        if (lower.contains("tool")) return "🔧";
        if (lower.contains("vehicle") || lower.contains("transport")) return "🚛";
        if (lower.contains("storage")) return "🏚️";
        return "🌿";
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public void updateData(List<ApiCategory> newCategories) {
        this.categories = newCategories;
        if (selectedPosition >= getItemCount()) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmoji;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvEmoji = itemView.findViewById(R.id.tvCategoryEmoji);
        }
    }
}
