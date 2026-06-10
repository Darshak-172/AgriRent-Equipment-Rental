package com.example.agrirent.activities;

import android.content.Intent;
import android.os.Bundle;

import com.example.agrirent.R;

public class OrderSuccessActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnViewOrders).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrdersActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
