package com.example.agrirent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.agrirent.R;
import com.example.agrirent.models.ProductItem;

import java.util.List;

public class ProductItemAdapter extends RecyclerView.Adapter<ProductItemAdapter.ViewHolder> {

    private Context context;
    private List<ProductItem> productList;

    public ProductItemAdapter(Context context, List<ProductItem> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem product = productList.get(position);

        holder.tvProductName.setText(product.getProductName());

        // Price with unit
        String unit = product.getUnit() != null ? "/" + product.getUnit() : "";
        holder.tvProductPrice.setText("₹" + String.format("%.0f", product.getPrice()) + unit);

        // Category badge
        if (holder.tvProductCategory != null) {
            holder.tvProductCategory.setText(product.getCategory() != null ? product.getCategory() : "");
        }

        // Subtitle (Using Unit or Category for default)
        if (holder.tvProductSubtitle != null) {
            String sub = product.getUnit() != null ? product.getUnit() : "Item";
            holder.tvProductSubtitle.setText(sub);
        }

        // Stock Info
        if (holder.tvProductStock != null) {
            int stock = product.getStock();
            if (stock > 0) {
                holder.tvProductStock.setText(stock + " Available");
                holder.tvProductStock.setTextColor(0xFF2E7D32); // Dark Green
            } else {
                holder.tvProductStock.setText("Out of Stock");
                holder.tvProductStock.setTextColor(0xFFD32F2F); // Red
            }
        }

        // Rating
        if (holder.tvAverageRatingCard != null) {
            holder.tvAverageRatingCard.setText(String.format(java.util.Locale.getDefault(), "%.1f", product.getAverageRating()));
        }

        // Image via Glide
        String imageUrl = product.getFirstImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_products)
                    .error(R.drawable.ic_products)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.ic_products);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.example.agrirent.activities.ProductDetailActivity.class);
            intent.putExtra("product", product);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateData(List<ProductItem> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvProductPrice, tvProductCategory, tvProductSubtitle, tvProductStock, tvAverageRatingCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage    = itemView.findViewById(R.id.ivProductImage);
            tvProductName     = itemView.findViewById(R.id.tvProductName);
            tvProductPrice    = itemView.findViewById(R.id.tvProductPrice);
            tvProductCategory = itemView.findViewById(R.id.tvProductCategory);
            tvProductSubtitle = itemView.findViewById(R.id.tvProductSubtitle);
            tvProductStock    = itemView.findViewById(R.id.tvProductStock);
            tvAverageRatingCard = itemView.findViewById(R.id.tvAverageRatingCard);
        }
    }
}
