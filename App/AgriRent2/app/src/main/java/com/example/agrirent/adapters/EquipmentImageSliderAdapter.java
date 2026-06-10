package com.example.agrirent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;

import java.util.List;

public class EquipmentImageSliderAdapter extends RecyclerView.Adapter<EquipmentImageSliderAdapter.SliderViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private boolean useFitCenter = false;

    public EquipmentImageSliderAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    public EquipmentImageSliderAdapter(Context context, List<String> imageUrls, boolean useFitCenter) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.useFitCenter = useFitCenter;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipment_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        String url = imageUrls.get(position);
        
        if (useFitCenter) {
            holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_agrirent_logo)
                    .error(R.drawable.ic_agrirent_logo)
                    .fitCenter()
                    .into(holder.imageView);
        } else {
            holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_agrirent_logo)
                    .error(R.drawable.ic_agrirent_logo)
                    .centerCrop()
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivSliderImage);
        }
    }
}
