package com.example.agrirent.models;

import java.util.ArrayList;
import java.util.List;

public class NotificationData {
    private static List<AppNotification> notifications;

    public static List<AppNotification> getNotifications() {
        if (notifications == null) {
            notifications = new ArrayList<>();
            notifications.add(new AppNotification("Booking Confirmed", "Your request for Mahindra Tractor has been approved.", "2 hours ago", true));
            notifications.add(new AppNotification("Order Dispatched", "Your order containing 2 bags of Organic Fertilizer has been dispatched.", "5 hours ago", true));
            notifications.add(new AppNotification("Payment Received", "Payment of ₹5,000 for your Harvester rental was successful.", "Yesterday", false));
            notifications.add(new AppNotification("Welcome to AgriRent!", "Start exploring high-quality farm equipment and products today.", "2 days ago", false));
        }
        return notifications;
    }

    public static int getUnreadCount() {
        int count = 0;
        for (AppNotification n : getNotifications()) {
            if (n.isUnread()) count++;
        }
        return count;
    }
}
