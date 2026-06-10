package com.example.agrirent.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.agrirent.R;
import java.util.List;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    public static class ForecastItem {
        public String dayName;
        public String conditionIcon;
        public String maxTemp;
        public String minTemp;
    }

    private final List<ForecastItem> items;

    public ForecastAdapter(List<ForecastItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastItem item = items.get(position);
        holder.tvDayName.setText(item.dayName);
        holder.tvConditionIcon.setText(item.conditionIcon);
        holder.tvMaxTemp.setText(item.maxTemp);
        holder.tvMinTemp.setText(item.minTemp);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvConditionIcon, tvMaxTemp, tvMinTemp;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvConditionIcon = itemView.findViewById(R.id.tvConditionIcon);
            tvMaxTemp = itemView.findViewById(R.id.tvMaxTemp);
            tvMinTemp = itemView.findViewById(R.id.tvMinTemp);
        }
    }
}
