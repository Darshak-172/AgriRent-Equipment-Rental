package com.example.agrirent.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.activities.EquipmentDetailActivity;
import com.example.agrirent.activities.ProductDetailActivity;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.models.SearchResult;
import com.example.agrirent.network.ApiClient;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private final Context context;
    private List<SearchResult> searchResults;

    public SearchResultAdapter(Context context, List<SearchResult> searchResults) {
        this.context = context;
        this.searchResults = searchResults;
    }

    public void updateData(List<SearchResult> newData) {
        this.searchResults = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = searchResults.get(position);

        holder.tvItemName.setText(result.getName());
        holder.tvItemCategory.setText(result.getCategoryName());
        
        if ("Equipment".equalsIgnoreCase(result.getType())) {
            String hourly = result.getHourlyPrice() != null ? String.format(java.util.Locale.getDefault(), "₹%.0f/hr", result.getHourlyPrice()) : "";
            String daily = result.getDailyPrice() != null ? String.format(java.util.Locale.getDefault(), "₹%.0f/day", result.getDailyPrice()) : "";
            holder.tvItemPrice.setText(hourly + " • " + daily);
        } else {
            holder.tvItemPrice.setText(String.format(java.util.Locale.getDefault(), "₹%.0f", result.getPrice()));
        }
        if (result.getLocation() != null && !result.getLocation().isEmpty()) {
            holder.tvItemLocation.setText("📍 " + result.getLocation());
            holder.tvItemLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvItemLocation.setVisibility(View.GONE);
        }

        if (result.getType() != null && !result.getType().isEmpty()) {
            holder.tvItemTypeBadge.setText(result.getType().toUpperCase());
            holder.tvItemTypeBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvItemTypeBadge.setVisibility(View.GONE);
        }

        if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
            
            // The API returns a relative path like /uploads/equipment/...
            // Use ApiClient.BASE_URL to prepend the host.
            String imageUrl = result.getImageUrl();
            if (imageUrl.startsWith("/")) {
                imageUrl = ApiClient.BASE_URL.substring(0, ApiClient.BASE_URL.length() - 1) + imageUrl;
            } else if (!imageUrl.startsWith("http")) {
                imageUrl = ApiClient.BASE_URL + imageUrl;
            }

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_agrirent_logo)
                    .error(R.drawable.ic_agrirent_logo)
                    .into(holder.ivItemImage);
        } else {
            holder.ivItemImage.setImageResource(R.drawable.ic_agrirent_logo);
        }

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || adapterPos >= searchResults.size()) return;
            SearchResult clicked = searchResults.get(adapterPos);

            if (isEquipmentResult(clicked)) {
                Equipment equipment = mapToEquipment(clicked);
                Intent intent = new Intent(context, EquipmentDetailActivity.class);
                intent.putExtra("equipment", equipment);
                context.startActivity(intent);
                return;
            }

            ProductItem product = mapToProduct(clicked);
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("product", product);
            context.startActivity(intent);
        });
    }

    private boolean isEquipmentResult(SearchResult result) {
        String type = result.getType() != null ? result.getType().trim() : "";
        if ("equipment".equalsIgnoreCase(type)) return true;
        if ("product".equalsIgnoreCase(type)) return false;
        return result.getHourlyPrice() != null;
    }

    private Equipment mapToEquipment(SearchResult result) {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(result.getEquipmentId());
        equipment.setEquipmentName(result.getName());
        equipment.setDescription(result.getDescription());
        equipment.setHourlyPrice(result.getHourlyPrice() != null ? result.getHourlyPrice() : 0.0);
        equipment.setDailyPrice(result.getDailyPrice() != null ? result.getDailyPrice() : result.getPrice());
        equipment.setLocation(result.getLocation());
        equipment.setCategoryName(result.getCategoryName());
        equipment.setStatus(result.getStatus());
        return equipment;
    }

    private ProductItem mapToProduct(SearchResult result) {
        ProductItem product = new ProductItem();
        Integer productId = result.getProductId();
        if (productId != null && productId > 0) {
            product.setProductId(productId);
        } else {
            product.setProductId(result.getEquipmentId());
        }
        product.setProductName(result.getName());
        product.setDescription(result.getDescription());
        product.setCategory(result.getCategoryName());
        product.setPrice(result.getPrice());
        product.setLocation(result.getLocation());
        product.setStock(1);
        product.setUnit("unit");
        product.setStatus(result.getStatus());
        product.setImageUrl(result.getImageUrl());
        return product;
    }

    @Override
    public int getItemCount() {
        return searchResults == null ? 0 : searchResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName, tvItemPrice, tvItemCategory, tvItemLocation, tvItemTypeBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemLocation = itemView.findViewById(R.id.tvItemLocation);
            tvItemTypeBadge = itemView.findViewById(R.id.tvItemTypeBadge);
        }
    }
}
