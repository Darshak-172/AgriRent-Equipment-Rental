package com.example.agrirent.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.activities.BookingDetailActivity;
import com.example.agrirent.models.BookingResponseDto;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final Context context;
    private final List<BookingResponseDto> bookingList;
    private final boolean isOwnerView;
    private final ApiService apiService;
    private final BookingActionListener listener;

    public interface BookingActionListener {
        void onActionSuccess();
    }

    public BookingAdapter(Context context, List<BookingResponseDto> bookingList, boolean isOwnerView, BookingActionListener listener) {
        this.context = context;
        this.bookingList = bookingList;
        this.isOwnerView = isOwnerView;
        this.listener = listener;
        this.apiService = ApiClient.getClient(context).create(ApiService.class);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingResponseDto booking = bookingList.get(position);

        holder.tvEquipmentName.setText(booking.getEquipmentName());
        holder.tvOtherPartyName.setText(isOwnerView ? booking.getFarmerName() : booking.getOwnerName());
        
        holder.tvTotalPrice.setText(String.format(Locale.getDefault(), "₹%.0f", booking.getTotalPrice()));

        if (booking.getEquipmentImageUrl() != null && !booking.getEquipmentImageUrl().isEmpty()) {
            Glide.with(context)
                .load(booking.getEquipmentImageUrl())
                .placeholder(R.drawable.ic_agrirent_logo)
                .error(R.drawable.ic_agrirent_logo)
                .centerCrop()
                .into(holder.ivEquipmentImage);
        } else {
            holder.ivEquipmentImage.setImageResource(R.drawable.ic_agrirent_logo);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, BookingDetailActivity.class);
            intent.putExtra("booking", booking);
            intent.putExtra("isOwnerView", isOwnerView);
            context.startActivity(intent);
        });

        // Format Dates
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            
            Date start = apiFormat.parse(booking.getStartDate());
            Date end = apiFormat.parse(booking.getEndDate());
            
            if (start != null && end != null) {
                holder.tvDatesRange.setText(displayFormat.format(start) + " - " + displayFormat.format(end));
            }
        } catch (ParseException e) {
            holder.tvDatesRange.setText(booking.getStartDate() + " to " + booking.getEndDate());
        }

        // Status Styling
        holder.tvStatus.setText(booking.getStatus());
        int bgRes, textCol;
        
        if ("Accepted".equalsIgnoreCase(booking.getStatus())) {
            bgRes = R.drawable.bg_status_active;
            textCol = R.color.status_active_text;
            holder.llOwnerActions.setVisibility(View.GONE);
        } else if ("Rejected".equalsIgnoreCase(booking.getStatus())) {
            bgRes = R.drawable.bg_status_inactive;
            textCol = R.color.status_inactive_text;
            holder.llOwnerActions.setVisibility(View.GONE);
        } else {
            bgRes = R.drawable.bg_status_pending;
            textCol = R.color.status_pending_text;
            holder.llOwnerActions.setVisibility(isOwnerView ? View.VISIBLE : View.GONE);
        }
        
        holder.tvStatus.setBackgroundResource(bgRes);
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, textCol));

        // Owner Actions
        if (isOwnerView && "Pending".equalsIgnoreCase(booking.getStatus())) {
            holder.btnAccept.setOnClickListener(v -> handleBookingAction(booking.getBookingId(), true));
            holder.btnReject.setOnClickListener(v -> handleBookingAction(booking.getBookingId(), false));
        }
    }

    private void handleBookingAction(int bookingId, boolean accept) {
        Call<String> call = accept ? apiService.acceptBooking(bookingId) : apiService.rejectBooking(bookingId);
        
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, response.body(), Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onActionSuccess();
                } else {
                    Toast.makeText(context, "Action failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public void updateData(List<BookingResponseDto> newList) {
        bookingList.clear();
        bookingList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipmentName, tvStatus, tvOtherPartyName, tvTotalPrice, tvDatesRange;
        com.google.android.material.imageview.ShapeableImageView ivEquipmentImage;
        LinearLayout llOwnerActions;
        MaterialButton btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            ivEquipmentImage = itemView.findViewById(R.id.ivEquipmentImage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOtherPartyName = itemView.findViewById(R.id.tvOtherPartyName);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvDatesRange = itemView.findViewById(R.id.tvDatesRange);
            llOwnerActions = itemView.findViewById(R.id.llOwnerActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
