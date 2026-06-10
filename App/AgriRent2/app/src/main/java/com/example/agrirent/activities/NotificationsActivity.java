package com.example.agrirent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.adapters.NotificationAdapter;
import com.example.agrirent.models.AppNotification;
import com.example.agrirent.models.NotificationData;

import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private NotificationAdapter adapter;
    private List<AppNotification> notificationList;
    private LinearLayout llEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        RecyclerView rvNotifications = findViewById(R.id.rvNotifications);
        llEmpty = findViewById(R.id.llEmpty);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        notificationList = NotificationData.getNotifications();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        updateEmptyState();

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                notificationList.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyState();
            }
        });
        itemTouchHelper.attachToRecyclerView(rvNotifications);
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            llEmpty.setVisibility(View.VISIBLE);
        } else {
            llEmpty.setVisibility(View.GONE);
        }
    }
}
