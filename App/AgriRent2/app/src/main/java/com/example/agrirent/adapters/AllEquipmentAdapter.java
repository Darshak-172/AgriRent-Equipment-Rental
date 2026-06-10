package com.example.agrirent.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.agrirent.R;
import com.example.agrirent.activities.EquipmentDetailActivity;
import com.example.agrirent.models.Equipment;

import java.util.List;

public class AllEquipmentAdapter extends RecyclerView.Adapter<AllEquipmentAdapter.ViewHolder> {

    private final Context context;
    private final List<Equipment> list;

    // Created ONCE — shared by all bind calls to avoid per-item allocation
    private static final ColorDrawable PLACEHOLDER = new ColorDrawable(0xFFFFFFFF);

    // Shared Glide RequestOptions — built once, reused for every image load
    // .override() downscales the image to card size BEFORE decoding, massively
    // reducing memory usage and load time vs loading full-resolution images.
    // NOTE: placeholder = white (shows during loading), error = app logo (shows on failure)
    private static final RequestOptions GLIDE_OPTIONS = new RequestOptions()
            .placeholder(PLACEHOLDER)              // white while loading
            .error(R.drawable.ic_agrirent_logo)    // logo if URL is broken/missing
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)  // cache both original + transformed
            .override(400, 300);                        // decode at card size, not full-res

    public AllEquipmentAdapter(Context context, List<Equipment> list) {
        this.context = context;
        this.list = list;
        setHasStableIds(false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipment_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Equipment e = list.get(position);

        h.tvName.setText(e.getEquipmentName());
        h.tvLocation.setText("📍 " + (e.getLocation() != null ? e.getLocation() : ""));

        h.tvHourlyPrice.setText("₹" + String.format(java.util.Locale.getDefault(), "%.0f", e.getHourlyPrice()) + "/hr");
        h.tvDailyPrice.setText("₹" + String.format(java.util.Locale.getDefault(), "%.0f", e.getDailyPrice()) + "/day");

        if (e.getCategoryName() != null) {
            h.tvCategory.setVisibility(View.VISIBLE);
            h.tvCategory.setText(e.getCategoryName());
        } else {
            h.tvCategory.setVisibility(View.GONE);
        }

        String imageUrl = e.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Reset view state before Glide loads (prevents recycled-view state leaking)
            h.ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            h.ivImage.setPadding(0, 0, 0, 0);
            h.ivImage.setBackgroundColor(0xFFFFFFFF);
            // Load with shared options: downscaled, cached; logo shown on load failure
            Glide.with(context)
                    .load(imageUrl)
                    .apply(GLIDE_OPTIONS)
                    .into(h.ivImage);
        } else {
            // No image — show app logo centred on white background
            h.ivImage.setBackgroundColor(0xFFFFFFFF);
            h.ivImage.setImageResource(R.drawable.ic_agrirent_logo);
            h.ivImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            h.ivImage.setPadding(32, 32, 32, 32);
        }

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EquipmentDetailActivity.class);
            intent.putExtra("equipment", e);
            context.startActivity(intent);
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder h) {
        super.onViewRecycled(h);
        // Clear Glide request when view is recycled to prevent stale images
        Glide.with(context).clear(h.ivImage);
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvLocation, tvHourlyPrice, tvDailyPrice, tvCategory;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage       = itemView.findViewById(R.id.ivEquipmentImageGrid);
            tvName        = itemView.findViewById(R.id.tvEquipmentNameGrid);
            tvLocation    = itemView.findViewById(R.id.tvEquipmentLocationGrid);
            tvHourlyPrice = itemView.findViewById(R.id.tvHourlyPriceGrid);
            tvDailyPrice  = itemView.findViewById(R.id.tvDailyPriceGrid);
            tvCategory    = itemView.findViewById(R.id.tvEquipmentCategoryGrid);
        }
    }
}
